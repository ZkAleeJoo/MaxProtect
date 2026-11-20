package org.zkaleejoo.protection.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class StorageExecutor {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final ExecutorService executor;
    private final Logger logger;

    public StorageExecutor(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "maxprotect-db");
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
                logger.warning("Waiting for queued database operations to finish before closing SQLite.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            executor.close();
        }
    }
}
