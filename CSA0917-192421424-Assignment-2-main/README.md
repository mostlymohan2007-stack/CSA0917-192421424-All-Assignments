# Java Assignment 2 (CO2) – Strings and Collections

## Requirements
- JDK 17 or later recommended.
- Each `.java` file is a standalone program. Compile and run from its folder.

## Compile and run
```bash
javac Q1_StringBenchmark.java && java Q1_StringBenchmark
javac Q2_ListBenchmark.java && java Q2_ListBenchmark
javac Q3_StudentHashSet.java && java Q3_StudentHashSet
javac Q4_PharmacyMaps.java && java Q4_PharmacyMaps
javac Q5_GenericLeaderboard.java && java Q5_GenericLeaderboard
javac Q6_HospitalTriage.java && java Q6_HospitalTriage
javac Q7_RedesignBenchmark.java && java Q7_RedesignBenchmark
```

## Notes
- Q1, Q2, Q4 and Q7 print measured nanosecond timings. Run them on your own computer
  several times and retain the raw output as experimental evidence.
- Timing results depend on CPU, JDK, warm-up, background load and JVM optimisation.
  Do not copy timings from another computer into your report.
- Q3 fixes the equals/hashCode contract and demonstrates HashSet de-duplication.
- Q5 demonstrates a reusable generic leaderboard using Comparator.
- Q6 demonstrates severity-first, FIFO-within-severity triage.
- Q7 compares linear list search with average constant-time HashMap lookup.

Repository name required by the assignment:
`CSA09-<RegisterNumber>-Assignment2`

Suggested folders: Q1, Q2, Q3, Q4, Q5, Q6, Q7. Include each source file,
your actual output screenshots, raw timing CSV/text, and this README.
