# CSA09-<RegisterNumber>-Assignment4

**Programming in Java - Assignment 4 (CO4): Event-Driven GUI Design** (individual assignment)

| | |
|---|---|
| Name | _fill in_ |
| Register number | _fill in_ |
| Java version | _fill in (`java -version`)_ |

> The programs, outputs and screenshots in this repository were produced with **Java 27.0.1 on Windows 11 (1920x1080 screen)** and **MySQL 8.4.9** with **Connector/J 8.4.0**.
> Re-run everything on your own computer, take your own screenshots and replace the timing values with your own measurements.
> For Tasks 1, 4 and 5 the assignment PDF does not print the supplied programs, so they were **reconstructed** from the description (labelled in each file). If your PPT shows different code, paste it in place of the reconstruction.

## Folder structure

```
Task01/  GUI design review      diagnosis/BadFeePaymentWindow.java (problem program)  FeeService, FeePaymentWindow (redesign)  FeeServiceDemo  fee_schema.sql  output/
Task02/  Requirement -> events  AppointmentDesk.java  DoctorLoadBench.java  doctors.sql  output/
Task03/  Sketch -> layout       LibrarySearch.java  LibraryLayoutTest.java  output/ (three window sizes)
Task04/  Event forensics        Buggy/ (3 files)  DrawingPad.java (TODO 1, TODO 2)  DrawingPadBuffered.java  DrawingPadTest.java  output/
Task05/  Four event styles      ConverterBase + 4 versions + ConverterMethodRef  ConverterTest.java  output/
Task06/  Design challenge       src/campus/{model,dao,service,ui,test}  sql/schema.sql  sql/seed_100000.sql  db.properties.example  output/
```

## Compile and run

Tasks 1-5 (no database needed except the optional parts):

```powershell
cd Task03
javac *.java
java LibrarySearch          # the application
java LibraryLayoutTest      # automatic test + screenshots (windows open briefly on your screen)
```

Tasks that use MySQL need the JDBC driver jar (download `mysql-connector-j-8.x.jar` from https://dev.mysql.com/downloads/connector/j/ or Maven Central) and the SQL scripts:

```powershell
mysql -u root -p < Task06\sql\schema.sql
mysql -u root -p campus_events < Task06\sql\seed_100000.sql     # optional: 100,000 rows for paging / load tests
cd Task06
copy db.properties.example db.properties                          # then edit your own MySQL password in db.properties
javac -cp mysql-connector-j-8.4.0.jar -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "mysql-connector-j-8.4.0.jar;out" campus.ui.App           # the application
java -cp "mysql-connector-j-8.4.0.jar;out" campus.test.ServiceTest   # requirement tests (run BEFORE the seed script)
java -cp "mysql-connector-j-8.4.0.jar;out" campus.ui.UiScenarioTest  # drives the real window and saves screenshots
java -cp "mysql-connector-j-8.4.0.jar;out" campus.test.LoadBenchmark # all-at-once vs paginated loading
```

Passwords are never written in the source: `Db.java` reads `db.properties`, `-Ddb.password=...` or the `DB_PASSWORD` environment variable.

## Task results (short notes)

| Task | Result |
|---|---|
| 1 | 11 problems diagnosed in `BadFeePaymentWindow`; redesign with `FeeService` (no Swing), `PaymentRepository` (in-memory + JDBC with PreparedStatement), separate Swing `Action` per button, `SwingWorker`. 11/11 service tests pass; window stays responsive (EDT answered in 0 ms during processing); JDBC part saved a payment and rejected hostile input. |
| 2 | Event table, containment tree and pseudocode in the report; `AppointmentDesk` logs the real listener sequence (activated, opened, itemStateChanged, actionPerformed, closing, deactivated, closed). Doctor lookup: Map 28.8 ns vs query 0.32 ms (shared connection) vs 16 ms (new connection). |
| 3 | Library search screen with 4 nested containers / 3 layout managers. At 520x380, 800x560, 1200x720 only the results list grows (224 -> 404 -> 564 px); search row (32 px) and button row (32 px) stay fixed. |
| 4 | Misnamed `mouseclicked` never runs; half-fix loses dots on minimise (319 -> 46 coloured pixels); `DrawingPad` (TODO 1 + TODO 2) keeps 319 after restore; BufferedImage version keeps 283. `@Override` turns the typo into a compile error. |
| 5 | Same converter in 4 styles (+ method reference/Action). Wiring code: frame 12 lines, inner class 16, anonymous 14, lambda 8. Enter key and result dialog work in every version. |
| 6 | Swing + JDBC + MySQL event registration, three layers, UNIQUE (student_id, event_id), capacity rule, SwingWorker. 18/18 requirement tests pass (incl. 10 simultaneous duplicate attempts: exactly 1 succeeds). 100,000-row test: load-all on EDT froze the window 1270 ms; paginated page 11-16 ms (OFFSET gets slower deep into the table: 541 ms at offset 99,975, keyset stays 11 ms). |

## Suggested commit history (at least one commit per task, spread over the week)

1. `Initial commit: README and folder structure`
2. `Task01: diagnose BadFeePaymentWindow`
3. `Task01: FeeService, repository and Swing Actions redesign`
4. `Task02: AppointmentDesk event trace and doctor-load benchmark`
5. `Task03: library search layout and resize test`
6. `Task04: buggy drawing pad, forensics and repaired DrawingPad`
7. `Task05: currency converter in four event styles`
8. `Task06: SQL schema, seed script and JDBC DAO`
9. `Task06: service layer and Swing screens`
10. `Task06: tests, load benchmark and screenshots`
11. `Add report and final screenshots`
