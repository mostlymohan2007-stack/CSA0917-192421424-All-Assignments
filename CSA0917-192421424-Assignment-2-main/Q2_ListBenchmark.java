import java.util.*;

public class Q2_ListBenchmark {
    private static volatile long sink;
    static final int[] SIZES={1000,10000,50000};
    static long time(Runnable task) {
        long start=System.nanoTime(); task.run(); return System.nanoTime()-start;
    }
    static void test(String name, List<Integer> list, int n) {
        long addEnd=time(()->{for(int i=0;i<n;i++) list.add(i);});
        long insertFront=time(()->{for(int i=0;i<Math.min(n,2000);i++) list.add(0,i);});
        long middle=time(()->{for(int i=0;i<Math.min(n,1000);i++)
            list.add(list.size()/2,i);});
        long randomAccess=time(()->{long sum=0; int count=Math.min(n,10000);
            for(int i=0;i<count;i++) sum+=list.get((i*31)%list.size()); sink=sum;});
        long iterate=time(()->{long sum=0; for(int x:list) sum+=x; sink=sum;});
        System.out.printf("%s,%d,%d,%d,%d,%d,%d%n",name,n,addEnd,insertFront,middle,randomAccess,iterate);
    }
    public static void main(String[] args) {
        System.out.println("Collection,N,AddEndNs,InsertFrontNs,InsertMiddleNs,IndexedAccessNs,IterationNs");
        for(int n:SIZES) {
            test("ArrayList",new ArrayList<>(),n);
            test("LinkedList",new LinkedList<>(),n);
        }
    }
}
