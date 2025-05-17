# MySQL Load Test

This is a Java application that performs load testing on a MySQL database by inserting random student records using multiple threads.

## Prerequisites

- Java 8 or higher
- Maven
- MySQL server running on localhost:3306
- A MySQL database named 'test'

## Configuration

The following parameters can be configured in `MySQLLoadTest.java`:

- `DB_URL`: JDBC URL for the MySQL database (default: "jdbc:mysql://localhost:3306/test")
- `USER`: MySQL username (default: "root")
- `PASS`: MySQL password (default: "root")
- `NUM_THREADS`: Number of concurrent threads (default: 10)
- `RECORDS_PER_THREAD`: Number of records each thread will insert (default: 10000)

## Building the Application

```bash
mvn clean package
```

## Running the Load Test

1. Make sure your MySQL server is running
2. Create a database named 'test' if it doesn't exist:
   ```sql
   CREATE DATABASE test;
   ```
3. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.MySQLLoadTest"
   ```

## Output

The application will output:
- Total number of successful inserts
- Total number of failed inserts
- Test duration in seconds
- Requests per second (RPS)

## Table Structure

The application will create a table named 'students' with the following structure:
```sql
CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    age INT
);
``` 