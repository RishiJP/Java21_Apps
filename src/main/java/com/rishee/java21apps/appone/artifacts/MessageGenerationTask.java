package com.rishee.java21apps.appone.artifacts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Random;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class MessageGenerationTask implements Runnable {
    private static final Random random = new Random();
    private final MessageQueue messageQueue;
    private final String threadName;

    @Override
    public void run() {

        //IntStream.rangeClosed(1, 10): Creates an IntStream that will iterate 10 times (from 1 to 10 inclusive), representing the 10 messages to be generated.
        IntStream.rangeClosed(1, 10)
                .forEach(i -> {
                    String randomMessage = generateRandomMessage();
                    long timestampMillis = Instant.now().toEpochMilli(); // Get current timestamp in milliseconds
                    messageQueue.enqueue(new MessageQueue.LogMessageRecord(timestampMillis, threadName, randomMessage));
                    log.trace("Thread '{}' generated and enqueued: {}", threadName, randomMessage);
                    try {
                        //small random delay is added to simulate some processing time
                        Thread.sleep(random.nextInt(10));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Thread '{}' interrupted.", threadName, e);
                    }
                });
        log.info("Thread '{}' finished generating messages.", threadName);
        messageQueue.enqueue(MessageQueue.END_OF_MESSAGES); // Enqueue the marker
    }

    /**
     * A helper method to generate a random lowercase string of a random length between 5 and 24 characters using streams
     *
     * @return
     */
    private String generateRandomMessage() {
        int length = random.nextInt(20) + 5;
        return random.ints(length, 'a', 'z' + 1)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
