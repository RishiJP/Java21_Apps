package com.rishee.java21apps.appone.artifacts;


import com.rishee.java21apps.utils.exception.Java21AppException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MessageConsumer implements Runnable {

    private final MessageQueue messageQueue;
    private final String outputFileName = "src/main/output/AppOneOutput.txt";
    private final Path outputPath = Paths.get(outputFileName);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss.SSS");
    private final ZoneId kolkataZoneId = ZoneId.of("Asia/Kolkata"); // Define the time-zone

    private final String appName = "AppOne"; // Consistent app name
    private ExecutorService writeExecutor; // Executor for asynchronous writing
    private volatile boolean firstWrite = true; // Flag to indicate the first write
    private final int numberOfProducers; // Inject the number of producers
    private final AtomicInteger endOfMessagesReceived = new AtomicInteger(0);

    public MessageConsumer() {
        this.messageQueue = null;
        this.writeExecutor = Executors.newSingleThreadExecutor(); // Initialize here
        this.numberOfProducers = 3;
    }

    public MessageConsumer(MessageQueue messageQueue, int numberOfProducers) {
        this.messageQueue = messageQueue;
        this.writeExecutor = Executors.newSingleThreadExecutor(); // Initialize here
        this.numberOfProducers = numberOfProducers;
    }

    @Override
    public void run() {
        log.info("Message consumer started. Writing to '{}'. Expecting {} end markers.", outputPath.toAbsolutePath(), numberOfProducers);
        OpenOption[] openOptionsAppend = {StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE};
        OpenOption[] openOptionsOverwrite = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

        try {
            while (endOfMessagesReceived.get() < numberOfProducers) {
                MessageQueue.LogMessageRecord record = messageQueue.dequeue();

                if (record == MessageQueue.END_OF_MESSAGES) {
                    log.info("Received end-of-messages marker ({} of {}).", endOfMessagesReceived.incrementAndGet(), numberOfProducers);
                    continue;
                }

                String formattedMessage = formatLogMessage(record);
                log.trace("Writing message : {}", formattedMessage);

                // Use the executor to write asynchronously
                writeExecutor.submit(() -> {
                    try {
                        if (firstWrite) {
                            Files.writeString(outputPath, formattedMessage + System.lineSeparator(), openOptionsOverwrite);
                            firstWrite = false;
                        } else {
                            Files.writeString(outputPath, formattedMessage + System.lineSeparator(), openOptionsAppend);
                        }
                    } catch (IOException e) {
                        log.error("Error writing to log file '{}'", outputPath.toAbsolutePath(), e);
                        throw new Java21AppException(appName, "Error writing to log file", e);
                    }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Message consumer interrupted.", e);
        } catch (Exception e) { // Add this catch block
            log.error("Unexpected exception in consumer thread.", e);
            throw new Java21AppException(appName, "Unexpected exception in consumer thread", e);
        } finally {
            log.info("Message consumer stopped. Shutting down writer.");
            shutdownAndAwaitTermination(writeExecutor);
        }
    }

    private String formatLogMessage(MessageQueue.LogMessageRecord record) {
        Instant instant = Instant.ofEpochMilli(record.timestampMs());
        ZonedDateTime kolkataDateTime = instant.atZone(kolkataZoneId); // Apply the time-zone
        String formattedTime = timeFormatter.format(kolkataDateTime);
        return String.format("%s [%s] - %s", formattedTime, record.threadName(), record.message());
    }

    private void shutdownAndAwaitTermination(ExecutorService executor) {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a second time if needed
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate in time.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}