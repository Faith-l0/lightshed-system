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

/**
 * Publishes alerts to the panic queue on a background thread, so a slow or dead broker never
 * slows down or breaks the caller. If the broker is down the alert is logged and dropped.
 */
public class JmsAlertPublisher implements AlertPublisher, AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConnectionFactory factory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "alert-publisher");
        t.setDaemon(true);
        return t;
    });

    public JmsAlertPublisher(String brokerUrl) {
        this(new ActiveMQConnectionFactory(brokerUrl));
    }

    public JmsAlertPublisher(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void publish(Alert alert) {
        try {
            executor.submit(() -> send(alert));
        } catch (RuntimeException e) {
            System.err.println("Could not queue alert: " + e.getMessage());
        }
    }

    private void send(Alert alert) {
        try (Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageProducer producer = session.createProducer(session.createQueue(Queues.PANIC))) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage(MAPPER.writeValueAsString(alert)));
        } catch (Exception e) {
            System.err.println("Could not publish alert (" + alert.message() + "): " + e.getMessage());
        }
    }

    /** Waits for queued alerts to be sent, then stops. */
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
