package za.co.lightshed.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.function.Consumer;

/**
 * Subscribes to stage changes with a DURABLE subscription: if the subscriber is offline when the
 * stage changes, the broker holds the message until it reconnects under the same name. The session
 * is transacted, so a handler that throws gets the message redelivered (and eventually dead-lettered).
 */
public class JmsStageChangeListener implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Connection connection;

    public JmsStageChangeListener(String brokerUrl, String subscriberName, Consumer<StageChange> handler)
            throws JMSException {
        connection = new ActiveMQConnectionFactory(brokerUrl).createConnection();
        connection.setClientID(subscriberName);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic topic = session.createTopic(Queues.STAGE_TOPIC);
        MessageConsumer consumer = session.createDurableConsumer(topic, subscriberName);
        consumer.setMessageListener(message -> {
            try {
                StageChange change = MAPPER.readValue(((TextMessage) message).getText(), StageChange.class);
                handler.accept(change);
                session.commit();
            } catch (Exception e) {
                System.err.println("Stage change handling failed, will retry: " + e.getMessage());
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
