import java.util.*;

class Medicine {
    final String code;
    final String name;
    int quantity;
    Medicine(String code,String name,int quantity) {
        this.code=code; this.name=name; this.quantity=quantity;
    }
    @Override public String toString() {
        return code+" | "+name+" | stock="+quantity;
    }
}
public class Q4_PharmacyMaps {
    static long lookup(Map<String,Medicine> map,int repetitions) {
        long start=System.nanoTime();
        for(int i=0;i<repetitions;i++) {
            Medicine m=map.get("MED"+(i%map.size()));
            if(m!=null) m.quantity++;
        }
        return System.nanoTime()-start;
    }
    public static void main(String[] args) {
        int[] sizes={100,10000,100000};
        System.out.println("Map,Medicines,100000LookupsNs,CodeOrderFirst,InsertionOrderFirst");
        for(int n:sizes) {
            Map<String,Medicine> hash=new HashMap<>();
            Map<String,Medicine> tree=new TreeMap<>();
            LinkedHashMap<String,Medicine> linked=new LinkedHashMap<>();
            for(int i=0;i<n;i++) {
                String code=String.format(Locale.ROOT,"MED%06d",i);
                Medicine m=new Medicine(code,"Medicine "+i,100);
                hash.put(code,m);
                tree.put(code,m);
                linked.put(code,m);
            }
            long h=lookup(hash,100000), t=lookup(tree,100000), l=lookup(linked,100000);
            System.out.printf("HashMap,%d,%d,%s,%s%n",n,h,
                firstKeySorted(hash),firstKeyInsertion(linked));
            System.out.printf("TreeMap,%d,%d,%s,%s%n",n,t,
                firstKeySorted(tree),firstKeyInsertion(linked));
            System.out.printf("LinkedHashMap,%d,%d,%s,%s%n",n,l,
                firstKeySorted(linked),firstKeyInsertion(linked));
        }
    }
    static String firstKeySorted(Map<String,Medicine> m) {
        return m.keySet().stream().sorted().findFirst().orElse("-");
    }
    static String firstKeyInsertion(LinkedHashMap<String,Medicine> m) {
        return m.keySet().iterator().next();
    }
}
