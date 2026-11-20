import java.util.*;

class Student {
    private final String regNo;
    private final String name;
    Student(String regNo,String name) {
        this.regNo=Objects.requireNonNull(regNo).trim().toUpperCase();
        this.name=Objects.requireNonNull(name);
    }
    @Override public boolean equals(Object obj) {
        if(this==obj) return true;
        if(!(obj instanceof Student other)) return false;
        return regNo.equals(other.regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return regNo+" - "+name; }
}
public class Q3_StudentHashSet {
    public static void main(String[] args) {
        Set<Student> students=new HashSet<>();
        students.add(new Student("IT101","Anu"));
        students.add(new Student("IT102","Bala"));
        students.add(new Student("IT101","Anu Kumar"));
        System.out.println("Unique students: "+students.size());
        System.out.println(students);
    }
}
