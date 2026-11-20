/**
 * INTENTIONALLY DOES NOT COMPILE.
 * Demonstrates what happens when a broader catch block is placed before a narrower one.
 * Compile with:  javac BrokenCatchOrder.java   and save the compiler message as evidence.
 */
public class BrokenCatchOrder {
    public static void main(String[] args) {
        try {
            Integer.parseInt("abc");
        } catch (IllegalArgumentException e) {        // broad (parent) first
            System.out.println("broad");
        } catch (NumberFormatException e) {           // narrow (child) later -> unreachable -> compile-time error
            System.out.println("narrow");
        }
    }
}
