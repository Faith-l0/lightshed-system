package za.co.lightshed.monitor;

import za.co.lightshed.common.Alert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Keeps the most recent alerts in memory, newest first when read. */
public class AlertStore {
    private final int capacity;
    private final Deque<Alert> alerts = new ArrayDeque<>();

    public AlertStore(int capacity) { this.capacity = capacity; }

    public synchronized void add(Alert alert) {
        alerts.addFirst(alert);
        while (alerts.size() > capacity) alerts.removeLast();
    }

    public synchronized List<Alert> recent() { return new ArrayList<>(alerts); }
}
