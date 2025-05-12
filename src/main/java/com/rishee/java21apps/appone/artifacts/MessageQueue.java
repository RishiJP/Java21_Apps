package com.rishee.java21apps.appone.artifacts;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MessageQueue {

    /**
     * java.util.concurrent.BlockingQueue is an excellent choice for this as it handles thread safety
     * for adding and removing elements and can also manage queue capacity if needed.
     * Using LinkedBlockingQueue as it's unbounded by default, but you can specify capacity
     */
    private final BlockingQueue<LogMessageRecord> queue = new LinkedBlockingQueue<>();
    public static final LogMessageRecord END_OF_MESSAGES = new LogMessageRecord(0, "SYSTEM", "END_OF_MESSAGES");

    // Java Record for the log message structure
    public record LogMessageRecord(long timestampMs, String threadName, String message) {
    }

    public void enqueue(LogMessageRecord record) {
        try {
            queue.put(record); // put() blocks if the queue is full (not the case here by default)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted.", e);
        }
    }

    public LogMessageRecord dequeue() throws InterruptedException {
        return queue.take(); // take() blocks if the queue is empty
    }

    public int size() {
        return queue.size();
    }

    // Optional: Method to check if the queue is empty
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}