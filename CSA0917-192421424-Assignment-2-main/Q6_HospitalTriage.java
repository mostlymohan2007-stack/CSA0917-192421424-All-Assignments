import java.util.*;

class Patient {
    final String name;
    final int severity;
    final long arrival;
    Patient(String name,int severity,long arrival) {
        if(severity<1 || severity>5) throw new IllegalArgumentException("Severity must be 1..5");
        this.name=name; this.severity=severity; this.arrival=arrival;
    }
    @Override public String toString() { return name+"(S"+severity+")"; }
}
public class Q6_HospitalTriage {
    private long counter=0;
    private final PriorityQueue<Patient> queue=new PriorityQueue<>(
        Comparator.comparingInt((Patient p)->p.severity)
                  .thenComparingLong(p->p.arrival));
    void admit(String name,int severity) {
        queue.add(new Patient(name,severity,counter++));
    }
    Patient next() { return queue.poll(); }
    public static void main(String[] args) {
        Q6_HospitalTriage triage=new Q6_HospitalTriage();
        triage.admit("Patient-A",3);
        triage.admit("Patient-B",1);
        triage.admit("Patient-C",2);
        triage.admit("Patient-D",1);
        triage.admit("Patient-E",5);
        triage.admit("Patient-F",3);
        triage.admit("Patient-G",2);
        triage.admit("Patient-H",4);
        System.out.println("Treatment order:");
        Patient p;
        while((p=triage.next())!=null) System.out.println(p);
    }
}
