import java.util.Locale;

public class Q1_StringBenchmark {
    private static volatile int sink;
    private static final int WARMUPS = 3;
    private static final int RUNS = 5;

    interface Builder { String build(int tickets, int fields); }

    static String usingPlus(int tickets, int fields) {
        String last = "";
        for (int t=0; t<tickets; t++) {
            String s = "";
            for (int f=0; f<fields; f++) s += "FIELD" + f + "=VALUE;";
            last = s;
        }
        return last;
    }
    static String usingStringBuilder(int tickets, int fields) {
        String last = "";
        for (int t=0; t<tickets; t++) {
            StringBuilder sb = new StringBuilder(fields * 16);
            for (int f=0; f<fields; f++) sb.append("FIELD").append(f).append("=VALUE;");
            last = sb.toString();
        }
        return last;
    }
    static String usingStringBuffer(int tickets, int fields) {
        String last = "";
        for (int t=0; t<tickets; t++) {
            StringBuffer sb = new StringBuffer(fields * 16);
            for (int f=0; f<fields; f++) sb.append("FIELD").append(f).append("=VALUE;");
            last = sb.toString();
        }
        return last;
    }
    static long measure(Builder b, int tickets, int fields) {
        long total=0;
        for (int i=0; i<WARMUPS; i++) sink = b.build(tickets,fields).length();
        for (int i=0; i<RUNS; i++) {
            long start=System.nanoTime();
            String result=b.build(tickets,fields);
            total += System.nanoTime()-start;
            sink=result.length();
        }
        return total/RUNS;
    }
    public static void main(String[] args) {
        int[][] workloads={{1,6},{10000,50},{2000,200}};
        Builder[] builders={Q1_StringBenchmark::usingPlus,
            Q1_StringBenchmark::usingStringBuilder,
            Q1_StringBenchmark::usingStringBuffer};
        String[] names={"String +","StringBuilder","StringBuffer"};
        System.out.println("Workload,Tickets,Fields,Approach,AverageTimeNs");
        for (int[] w:workloads) {
            for (int i=0;i<builders.length;i++) {
                long ns=measure(builders[i],w[0],w[1]);
                System.out.printf(Locale.US,"%s,%d,%d,%s,%d%n",
                    w[0]+"x"+w[1],w[0],w[1],names[i],ns);
            }
        }
        System.out.println("Note: run several times on the same machine/JDK; timings vary.");
    }
}
