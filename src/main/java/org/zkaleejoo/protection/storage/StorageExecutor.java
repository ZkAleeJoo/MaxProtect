package org.zkaleejoo.protection.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class StorageExecutor {

    private static final int POOL_SIZE = 2;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final ExecutorService executor;
    private final Logger logger;

    public StorageExecutor(Logger logger) {
        this.logger = logger;
        AtomicInteger counter = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(POOL_SIZE, runnable -> {
            Thread thread = new Thread(runnable, "argosprotect-db-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warning("Database executor did not terminate in time, forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
