import java.awt.event.*;

/**
 * INTENTIONALLY DOES NOT COMPILE. Shows how @Override catches the misspelt listener method at compile time.
 * Compile with  javac BrokenOverride.java  and keep the error message as evidence.
 */
public class BrokenOverride {
    MouseAdapter adapter = new MouseAdapter() {
        @Override
        public void mouseclicked(MouseEvent e) {         // wrong name -> "method does not override or implement a method from a supertype"
            System.out.println("clicked");
        }
    };
}
