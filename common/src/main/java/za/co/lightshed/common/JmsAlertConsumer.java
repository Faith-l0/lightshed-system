package za.co.lightshed.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.function.Consumer;

/**
 * Listens on the panic queue. Uses a transacted session: if the handler throws or the message
 * can't be parsed, the message is rolled back and redelivered. After the broker's retry limit it
 * lands in the dead-letter queue (ActiveMQ.DLQ) instead of being lost or blocking the queue.
 */
public class JmsAlertConsumer implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Connection connection;

    public JmsAlertConsumer(String brokerUrl, Consumer<Alert> handler) throws JMSException {
        connection = new ActiveMQConnectionFactory(brokerUrl).createConnection();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        MessageConsumer consumer = session.createConsumer(session.createQueue(Queues.PANIC));
        consumer.setMessageListener(message -> {
            try {
                Alert alert = MAPPER.readValue(((TextMessage) message).getText(), Alert.class);
                handler.accept(alert);
                session.commit();
            } catch (Exception e) {
                System.err.println("Alert handling failed, will retry: " + e.getMessage());
                try {
                    session.rollback();
                } catch (JMSException ignored) {
                    // nothing more we can do
                }
            }
        });
        connection.start();
    }

    @Override
    public void close() throws JMSException {
        connection.close();
    }
}
