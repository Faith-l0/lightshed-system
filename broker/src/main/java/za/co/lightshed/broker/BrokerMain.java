package za.co.lightshed.broker;

import org.apache.activemq.broker.BrokerService;

/** Starts a local ActiveMQ broker on tcp://localhost:61616. Start this before Schedule and Monitor. */
public class BrokerMain {
    public static void main(String[] args) throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName("lightshed");
        broker.setPersistent(false);   // in-memory: queued alerts are lost if the broker restarts
        broker.setUseJmx(false);
        broker.addConnector("tcp://localhost:61616");
        broker.start();
        System.out.println("Broker running on tcp://localhost:61616");
        broker.waitUntilStopped();
    }
}
