package com.economy.plugin.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TransactionLogger implements Runnable, AutoCloseable {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final File file;
    private final boolean enabled;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor;
    private volatile boolean running = true;

    public TransactionLogger(File dataDir, boolean enabled, String fileName) {
        this.enabled = enabled;
        if (enabled) {
            if (dataDir != null && !dataDir.exists()) {
                dataDir.mkdirs();
            }
            this.file = new File(dataDir, fileName == null || fileName.isEmpty() ? "transactions.log" : fileName);
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "true-economy-transactions");
                t.setDaemon(true);
                return t;
            });
            this.executor.submit(this);
        } else {
            this.file = null;
            this.executor = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void log(TransactionType type, String from, String to, double amount, String detail) {
        if (!enabled || type == null) {
            return;
        }
        StringBuilder line = new StringBuilder(96)
                .append('[').append(LocalDateTime.now().format(TS)).append("] ")
                .append(type.getId().toUpperCase())
                .append(" | ").append(from == null ? "-" : from)
                .append(" -> ").append(to == null ? "-" : to)
                .append(" | amount=").append(String.format("%.2f", amount));
        if (detail != null && !detail.isEmpty()) {
            line.append(" | ").append(detail);
        }
        queue.offer(line.toString());
    }

    public void log(TransactionType type, String from, String to, double amount) {
        log(type, from, to, amount, null);
    }

    @Override
    public void run() {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (running || !queue.isEmpty()) {
                String line = queue.poll(500, TimeUnit.MILLISECONDS);
                if (line != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("[TrueEconomy] Failed to write transaction log: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        this.running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}