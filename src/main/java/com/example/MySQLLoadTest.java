package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQLLoadTest {
    private static final Logger logger = LoggerFactory.getLogger(MySQLLoadTest.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3307/load_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true&useLocalSessionState=true&useLocalTransactionState=true&cacheCallableStmts=true&cacheServerConfiguration=true&elideSetAutoCommits=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final int NUM_THREADS = 200; // Increased number of threads
    private static final int RECORDS_PER_THREAD = 100000;
    private static final AtomicInteger successfulInserts = new AtomicInteger(0);
    private static final AtomicInteger failedInserts = new AtomicInteger(0);
    private static final AtomicLong lastReportTime = new AtomicLong(0);
    private static final AtomicInteger lastReportCount = new AtomicInteger(0);
    private static volatile boolean isRunning = true;
    private static HikariDataSource dataSource;

    public static void main(String[] args) {
        try {
            setupConnectionPool();
            createTable();
            runLoadTest();
        } catch (Exception e) {
            logger.error("Error in load test", e);
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }

    private static void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        config.setMaximumPoolSize(NUM_THREADS);
        config.setMinimumIdle(NUM_THREADS);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    private static void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "age INT) ENGINE=InnoDB";
            conn.createStatement().execute(createTableSQL);
        }
    }

    private static void reportProgress() {
        long currentTime = System.currentTimeMillis();
        long startTime = lastReportTime.get();
        if (startTime == 0) {
            lastReportTime.set(currentTime);
            return;
        }

        int currentCount = successfulInserts.get();
        int countSinceLastReport = currentCount - lastReportCount.get();
        double durationSeconds = (currentTime - startTime) / 1000.0;
        double currentRps = countSinceLastReport / durationSeconds;

        logger.info("Current RPS: {}, Total inserts: {}", 
                   String.format("%.2f", currentRps), currentCount);

        lastReportTime.set(currentTime);
        lastReportCount.set(currentCount);
    }

    private static void startProgressReporter() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning) {
                reportProgress();
            } else {
                scheduler.shutdown();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private static void runLoadTest() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            NUM_THREADS, NUM_THREADS,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(NUM_THREADS),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        long startTime = System.currentTimeMillis();
        lastReportTime.set(startTime);

        startProgressReporter();

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    insertRecords();
                } catch (SQLException e) {
                    logger.error("Error in thread", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        isRunning = false;
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;
        int totalInserts = successfulInserts.get();
        double rps = totalInserts / durationSeconds;

        logger.info("Load test completed:");
        logger.info("Total successful inserts: {}", totalInserts);
        logger.info("Total failed inserts: {}", failedInserts.get());
        logger.info("Total duration: {} seconds", String.format("%.2f", durationSeconds));
        logger.info("Average RPS: {}", String.format("%.2f", rps));
    }

    private static void insertRecords() throws SQLException {
        String insertSQL = "INSERT INTO students (name, age) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                try {
                    pstmt.setString(1, generateRandomName());
                    pstmt.setInt(2, generateRandomAge());
                    pstmt.executeUpdate();
                    successfulInserts.incrementAndGet();
                } catch (SQLException e) {
                    failedInserts.incrementAndGet();
                    logger.error("Error inserting record", e);
                }
            }
        }
    }

    private static String generateRandomName() {
        // Reduced name length for better performance
        String[] names = {"John", "Jane", "Mike", "Sara", "Dave", "Lisa", "Tom", "Emma"};
        return names[(int) (Math.random() * names.length)];
    }

    private static int generateRandomAge() {
        return 18 + (int) (Math.random() * 50);
    }
} 