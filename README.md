# MySQL Load Test

This is a Java application that performs load testing on a MySQL database by inserting random student records using multiple threads. The application is optimized to achieve high RPS (Requests Per Second) using connection pooling and optimized MySQL configuration.

## Prerequisites

- Java 8 or higher
- Maven
- Docker
- MySQL container running on localhost:3307

## MySQL Container Optimization

To achieve high RPS (10K+), the following optimizations were applied to the MySQL container configuration at `/etc/mysql/conf.d/performance.cnf`:

```ini
[mysqld]
# Connection and Thread Settings
max_connections = 5000
thread_cache_size = 128
table_open_cache = 4000
table_open_cache_instances = 16

# InnoDB Buffer Pool Settings
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_log_buffer_size = 64M
innodb_flush_log_at_trx_commit = 0
innodb_flush_method = O_DIRECT
innodb_thread_concurrency = 0
innodb_io_capacity = 2000
innodb_io_capacity_max = 4000

# Performance Schema
performance_schema = OFF
```

### Configuration Explanation

1. **Connection Settings**:
   - `max_connections = 5000`: Allows up to 5000 simultaneous connections
   - `thread_cache_size = 128`: Caches thread handlers for better performance
   - `table_open_cache = 4000`: Increases number of open tables cache

2. **InnoDB Optimizations**:
   - `innodb_buffer_pool_size = 1G`: Allocates 1GB for buffer pool
   - `innodb_flush_log_at_trx_commit = 0`: Maximum performance mode (slight durability trade-off)
   - `innodb_flush_method = O_DIRECT`: Bypasses OS cache for better I/O performance
   - `innodb_thread_concurrency = 0`: Let InnoDB handle thread concurrency automatically

## Application Configuration

The Java application is optimized with:

1. **Connection Pooling (HikariCP)**:
   - Maximum pool size matches thread count
   - Optimized pool settings for high-performance

2. **Thread Configuration**:
   - 200 concurrent threads
   - 100,000 records per thread
   - ThreadPoolExecutor with CallerRunsPolicy

3. **JDBC URL Optimization**:
```
jdbc:mysql://localhost:3307/load_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true&useLocalSessionState=true&useLocalTransactionState=true&cacheCallableStmts=true&cacheServerConfiguration=true&elideSetAutoCommits=true
```

## Building the Application

```bash
mvn clean package
```

## Running the Load Test

1. Make sure your MySQL container is running with the optimized configuration
2. Create a database named 'load_test' if it doesn't exist:
   ```sql
   CREATE DATABASE load_test;
   ```
3. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.MySQLLoadTest"
   ```

## Output

The application will output:
- Current RPS every 5 seconds
- Total successful inserts
- Total failed inserts
- Test duration in seconds
- Average RPS achieved

## Table Structure

The application creates a table named 'students' with the following structure:
```sql
CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    age INT
) ENGINE=InnoDB;
```

## Performance Monitoring

The application provides real-time RPS monitoring every 5 seconds during the test execution. This helps in:
- Monitoring current performance
- Identifying performance patterns
- Detecting performance degradation
- Verifying if target RPS is achieved

## Troubleshooting

If you're not achieving the target RPS:
1. Check MySQL error log for potential issues
2. Verify system resources (CPU, Memory, Disk I/O)
3. Adjust thread count and records per thread
4. Consider increasing innodb_buffer_pool_size if memory allows
5. Monitor system swap usage 