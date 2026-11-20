import java.util.*;

/**
 * Q7 comparative experiment:
 * Original: scan a list to find a medicine by code.
 * Redesign: use a HashMap keyed by code.
 * Run with same generated data and lookup sequence.
 */
public class Q7_RedesignBenchmark {
    static class Item {
        final String code;
        int quantity;
        Item(String code,int quantity){this.code=code;this.quantity=quantity;}
    }
    static volatile int sink;
    public static void main(String[] args) {
        int[] sizes={1000,10000,50000};
        int lookups=100000;
        System.out.println("N,ArrayScanNs,HashMapNs,ArrayScanComparisons,HashMapGets");
        for(int n:sizes) {
            List<Item> list=new ArrayList<>();
            Map<String,Item> map=new HashMap<>();
            for(int i=0;i<n;i++) {
                String code="P"+i;
                Item item=new Item(code,i);
                list.add(item); map.put(code,item);
            }
            long comparisons=0;
            long start=System.nanoTime();
            for(int i=0;i<lookups;i++) {
                String key="P"+(i%n);
                for(Item item:list) {
                    comparisons++;
                    if(item.code.equals(key)){sink=item.quantity;break;}
                }
            }
            long arrayNs=System.nanoTime()-start;
            start=System.nanoTime();
            for(int i=0;i<lookups;i++) {
                Item item=map.get("P"+(i%n));
                if(item!=null)sink=item.quantity;
            }
            long mapNs=System.nanoTime()-start;
            System.out.printf("%d,%d,%d,%d,%d%n",n,arrayNs,mapNs,comparisons,lookups);
        }
    }
}
