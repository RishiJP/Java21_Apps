package com.rishee.java21apps.appone;

import lombok.extern.slf4j.Slf4j;
import com.rishee.java21apps.appone.artifacts.MessageConsumer;
import com.rishee.java21apps.appone.artifacts.MessageQueue;
import com.rishee.java21apps.appone.artifacts.MessageGenerationTask;
import com.rishee.java21apps.utils.exception.Java21AppException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
public class AppOneStarterMain {

    private final static String appName = "AppOne";
    private final Random random = new Random();

    public static void main(String[] args) {
        log.info("logback.xml Starting Execution for {} **** ****", appName);
        new AppOneStarterMain().executeAppLogic();
    }

    public void executeAppLogic() {

        int producerThreadCount = generateNumberOfThreads();
        MessageQueue messageQueue = new MessageQueue();
        MessageConsumer consumer = new MessageConsumer(messageQueue, producerThreadCount);

        ExecutorService producerExecutor = Executors.newFixedThreadPool(producerThreadCount);
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();

        try {
            // Start the producer threads
            List<MessageGenerationTask> producerTasks = IntStream.rangeClosed(1, producerThreadCount)
                    .mapToObj(i -> new MessageGenerationTask(messageQueue, "Producer-" + i))
                    .toList();

            producerTasks.forEach(producerExecutor::submit);

            // Start the consumer thread
            consumerExecutor.submit(consumer);

            // Wait for all producer threads to complete
            producerExecutor.shutdown();
            if (!producerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                producerExecutor.shutdownNow();
                if (!producerExecutor.awaitTermination(300, TimeUnit.SECONDS)) {
                    log.error("Producer executor did not terminate in time.");
                }
            }

            log.info("All producer threads finished. Signaling consumer to stop (if needed).");

            // We don't need to explicitly shut down the consumer executor here immediately.
            // The consumer will shut down its own executor after processing all messages.

            // Wait for the consumer to finish (optional, but good practice for a clean exit)
            consumerExecutor.shutdown();
            if (!consumerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
                log.error("Consumer executor did not terminate in time.");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} application interrupted.", appName, e);
            throw new Java21AppException(appName, "Application interrupted", e);
        } catch (Java21AppException e) {
            log.error("{} application encountered an error: {}", appName, e.getMessage(), e);
        } finally {
            log.info("{} application shutting down.", appName);
        }
    }

    private int generateNumberOfThreads() {
        return random.nextInt(5) + 3; // Generates a number between 3 and 7 (inclusive)
    }

    @Deprecated
    private void shutdownAndAwaitTermination(ExecutorService executor) {
        executor.shutdown();
        try {
            while (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(300, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate in time.");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("Interrupted during executor shutdown.");
        }
    }
}
