package za.co.lightshed.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Publishes stage changes to the stage topic in the background; never throws into the caller. */
public class JmsStageChangePublisher implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConnectionFactory factory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "stage-change-publisher");
        t.setDaemon(true);
        return t;
    });

    public JmsStageChangePublisher(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    public void publish(StageChange change) {
        try {
            executor.submit(() -> send(change));
        } catch (RuntimeException e) {
            System.err.println("Could not queue stage change: " + e.getMessage());
        }
    }

    private void send(StageChange change) {
        try (Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageProducer producer = session.createProducer(session.createTopic(Queues.STAGE_TOPIC))) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage(MAPPER.writeValueAsString(change)));
        } catch (Exception e) {
            System.err.println("Could not publish stage change to " + change.current() + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
