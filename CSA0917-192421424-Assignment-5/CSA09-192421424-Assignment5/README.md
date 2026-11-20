# CSA09-<RegisterNumber>-Assignment5

**Programming in Java - Assignment 5 (CO5): Data Persistence Engineering - Files and Databases** (individual assignment)

| | |
|---|---|
| Name | _fill in_ |
| Register number | _fill in_ |
| Java version | _fill in (`java -version`)_ |

> Programs, outputs and measurements in this repository were produced with **Java 27.0.1 on Windows 11** and **MySQL 8.4.9** (local test server on port 3307) with **Connector/J 8.4.0**.
> Re-run everything on your own computer and replace the timing values with your own measurements (they depend on CPU, disk, JDK and MySQL settings).
> The PDF does not print the supplied programs `PassStore` and `PassLoader`; they are **reconstructed** around the described TODOs (labelled in the files).
> The PDF refers to the *SIMATS Engineering Standard for Algorithmic Pseudocode* but does not include it; the pseudocode uses one consistent structured style - adapt headings if the standard asks for others.

## Folder structure

```
PartA_Pseudocode/            A1-A5 pseudocode as text files (the full design, tables and explanations are in the report)
Task06_B1_FileStore/         BusPass, PassStore (TODO 1), PassStoreDemo, LineCountBench, SearchBench      output/
Task07_B2_Adversarial/       test_database.sql, LoginDemo (unsafe vs safe), DamagedDataDemo                output/
Task08_B3_JdbcPerformance/   PassLoader (TODO 1-3), Insert10Demo, InsertBench, FailureDemo, BulkLoadBench  output/
Task09_B4_Decision/          MakeTestFile, Migrate (DatabaseMetaData), DecisionBench                       output/
```

(Part A tasks A1-A5 are Task01-Task05 of the numbering in the assignment; B1-B4 are Task06-Task09.)

## Prepare the local test database (used only for this assignment)

```powershell
mysql -u root -p < Task07_B2_Adversarial\test_database.sql        # creates transport_test with app_user and bus_pass
```
The programs read the connection from the JVM properties `-Ddb.url=... -Ddb.user=... -Ddb.password=...`
(defaults: `jdbc:mysql://127.0.0.1:3307/transport_test`, user `root`, empty password - change them for your own server; never commit your real password).
For LOAD DATA LOCAL (BulkLoadBench) the server needs `SET GLOBAL local_infile = 1`.

## Compile and run

Download `mysql-connector-j-8.x.jar` (Maven Central / dev.mysql.com) into each task folder or point `-cp` at it.

```powershell
cd Task06_B1_FileStore
javac *.java
java PassStoreDemo                 # test table, supplied example (P1003 record, then null)
java SearchBench                   # search time 1,000 -> 100,000 records, scan vs HashMap
java LineCountBench 1 10 50        # 1/10/50 MB, buffered vs unbuffered (the 50 MB unbuffered run takes about 70 s)

cd ..\Task07_B2_Adversarial
javac -cp mysql-connector-j-8.4.0.jar *.java
java -cp "mysql-connector-j-8.4.0.jar;." LoginDemo jdbc:mysql://127.0.0.1:3307/transport_test root ""
java DamagedDataDemo

cd ..\Task08_B3_JdbcPerformance
javac -cp mysql-connector-j-8.4.0.jar *.java
java -cp "mysql-connector-j-8.4.0.jar;." Insert10Demo
java -cp "mysql-connector-j-8.4.0.jar;." FailureDemo
java -cp "mysql-connector-j-8.4.0.jar;." BulkLoadBench
java -cp "mysql-connector-j-8.4.0.jar;." InsertBench 100000        # about 15 minutes: auto-commit variants are slow

cd ..\Task09_B4_Decision
javac -cp mysql-connector-j-8.4.0.jar *.java
java MakeTestFile 100000 passes_100000.txt
java -cp "mysql-connector-j-8.4.0.jar;." Migrate passes_100000.txt jdbc:mysql://127.0.0.1:3307/transport_test root "" rejects.txt
java -Xmx4g -cp "mysql-connector-j-8.4.0.jar;." DecisionBench
```

## Suggested commit history (at least one commit per task, spread over the week)

1. `Initial commit: README, folder structure and test database script`
2. `A1: file format design and pseudocode` (PartA_Pseudocode)
3. `A2: buffered I/O prediction and redesign`
4. `A3: generic Dao design and Student mapping`
5. `A4: serialization design`
6. `A5: transaction and login pseudocode`
7. `B1: BusPass and PassStore with TODO 1`
8. `B1: line-count and search benchmarks`
9. `B2: unsafe vs safe login and damaged-data tests`
10. `B3: PassLoader with TODO 1-3 and 10-record demo`
11. `B3: insert benchmarks, failure demo and bulk load comparison`
12. `B4: migration program with DatabaseMetaData`
13. `B4: decision benchmark and final report`
