# CSA09-<RegisterNumber>-Assignment3

**Programming in Java - Assignment 3 (CO3): Exception Handling and Multithreading**

| | Student A | Student B |
|---|---|---|
| Name | _fill in_ | _fill in_ |
| Register number | _fill in_ | _fill in_ |
| Java version | _fill in (`java -version`)_ | _fill in_ |

> The programs and outputs in this repository were prepared and executed with **Java 27.0.1 (Oracle JDK) on Windows 11, 12 logical cores**.
> Each student must re-run everything on their own machine, add their own screenshots to the `output/` folders
> and replace the timing values with their own measurements (timings depend on CPU, JDK, JIT warm-up and background load).

## How to compile and run (any question)

```powershell
cd Q1
javac *.java
java ATMWithdrawal
```

All programs are in the default package; no external libraries are needed (Java 17 or newer, because the code uses switch expressions and `Files.readString`).

## Folder structure

```
Q1/  ATMWithdrawal.java  ATMNoExceptions.java  ExceptionCostBench.java  demo_compile_error/BrokenCatchOrder.java  output/
Q2/  ExamPortal.java  CompleteTheCode/StorageDemo.java  CompleteTheCode/StorageDemoNoCause.java  output/
Q3/  SalesReport.java  input/  output/
Q4/  ScannerStations.java  output/
Q5/  SeatBooking.java  SeatBookingUnsafe.java  SeatBookingStrategies.java  output/
Q6/  TokenCounter.java  TokenCounterIf.java  TokenCounterTrace.java  WalletDeadlock.java  MessBlockingQueue.java  output/
Q7/  ResultProcessor.java  output/
```

Compile each folder separately (Q2 has two sub-folders because the given `StorageDemo` code re-declares `ExamException`).

## Task notes

### Q1 - Bank ATM simulation
`javac ATMWithdrawal.java` then `java ATMWithdrawal` (8-row test table), `java ATMWithdrawal uncaught` (uncaught stack trace).
`java ATMNoExceptions` (error-code alternative), `java ExceptionCostBench` (cost of exceptions vs error codes).
`demo_compile_error/BrokenCatchOrder.java` **intentionally does not compile** (broader catch before narrower); the compiler message is in `output/`.
Result: all 8 tests were handled by the expected catch block and `finally` ran 8 times.

### Q2 - Online examination portal
`java ExamPortal` runs seven scenarios (one per exception, chaining with and without cause).
`CompleteTheCode/StorageDemo.java` is the given code with TODO 1 completed - expected output reproduced exactly.
`StorageDemoNoCause` shows the NullPointerException that happens when the cause is not passed.

### Q3 - Resource safety
`java SalesReport` writes the `input/*.csv` files, compares the three versions (identical report) and counts open files after a failure:
manual-close-not-in-finally leaks 1 reader + 1 writer; try/finally and try-with-resources leak 0. Also shows closing order and suppressed exceptions.
Student A must add VS Code debugger screenshots (breakpoint on `long t = totalOf(r, s);`).

### Q4 - Threads
`java ScannerStations [compare|order|priority|prioritycpu|lifecycle|runvsstart|daemon|executor]`.
Run `order` and `priority` at least 10 times (a PowerShell loop: `1..10 | % { java ScannerStations order }`).

### Q5 - Race condition
`java SeatBookingUnsafe` (race: ~90-97 seats sold instead of 10), `java SeatBooking` (Complete-the-Code, prints `Seats sold: 10`),
`java SeatBookingStrategies` (1000-round correctness test and contention timing for six strategies).
Note: the race window is only nanoseconds, so the unsafe program models a ~1 ms database round-trip with `Thread.sleep(1)` and releases all threads together with a latch.

### Q6 - Deadlock and producer-consumer
`java TokenCounter` (`Sum of tokens collected: 210 (expected 210)`), `java TokenCounterIf` (while -> if experiment, may print wrong sums),
`java TokenCounterTrace`, `java MessBlockingQueue`, `java WalletDeadlock deadlock|ordered|trylock`.

### Q7 - Concurrent fault-tolerant processor
`java ResultProcessor selftest` (12 hand-checked files), `java ResultProcessor bench C:\temp\q7data` (generates 10,000 files and measures pools 1, 2, 4, 8, 16, 32; takes about 3-4 minutes),
`java ResultProcessor alt C:\temp\q7data` (ExecutorService vs parallelStream vs thread-per-file).
The simulated-latency columns use `Thread.sleep` and are labelled as simulated in the output and in the report.

## Suggested commit history (at least one commit per task, spread over the week)

1. `Initial commit: README and folder structure`
2. `Q1: ATM withdrawal with try/catch/finally and test table`
3. `Q1: error-code alternative and exception cost benchmark`
4. `Q2: user-defined exception hierarchy and 3-layer propagation`
5. `Q2: complete-the-code StorageDemo and chaining evidence`
6. `Q3: sales report - manual close vs try-with-resources`
7. `Q4: scanner stations with Thread and Runnable`
8. `Q4: priority, daemon and executor experiments`
9. `Q5: seat booking race and synchronized fix`
10. `Q5: synchronization strategies comparison`
11. `Q6: deadlock, lock ordering and token counter`
12. `Q6: ArrayBlockingQueue and tryLock alternatives`
13. `Q7: concurrent result processor and measurements`
14. `Add screenshots and final report`
