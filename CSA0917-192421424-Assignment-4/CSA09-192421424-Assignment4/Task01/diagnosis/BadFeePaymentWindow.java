import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

/**
 * Task 1 - THE PROBLEM PROGRAM (reconstructed from the scenario in the assignment:
 * "one listener for every button and all logic inside the event handler").
 * The assignment PDF does not print this code, so this is a faithful reconstruction of the described design.
 * It COMPILES but is NOT meant to be used: every numbered comment marks a design problem diagnosed in the report.
 */
public class BadFeePaymentWindow extends JFrame implements ActionListener {   // (1) window is also the listener AND the logic
    JTextField id = new JTextField(10), amount = new JTextField(8);
    JButton pay = new JButton("Pay"), receipt = new JButton("Receipt"), clear = new JButton("Clear");
    JLabel status = new JLabel(" ");

    BadFeePaymentWindow() {
        setLayout(new FlowLayout());
        add(new JLabel("Student ID")); add(id); add(new JLabel("Amount")); add(amount);
        add(pay); add(receipt); add(clear); add(status);
        pay.addActionListener(this);          // (2) the same listener object is registered for every button
        receipt.addActionListener(this);
        clear.addActionListener(this);
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "Pay") {                                         // (3) String compared with == (reference, not content)
            try {
                int amt = Integer.parseInt(amount.getText());       // (4) no validation: "", "abc", "-5", "1e3" throw or pass
                Thread.sleep(3000);                                  // (5) sleep() on the Event Dispatch Thread freezes the whole window
                Connection c = DriverManager.getConnection("jdbc:mysql://localhost/fees", "root", "root");   // (6) DB access on the EDT, hard-coded password
                Statement s = c.createStatement();
                s.executeUpdate("INSERT INTO payments VALUES ('" + id.getText() + "', " + amt + ")");        // (7) SQL built by string concatenation -> SQL injection
                // (8) Statement and Connection are never closed -> resource leak
            } catch (Exception ex) {
                // (9) exception swallowed: user sees nothing, developer sees nothing
            }
            // (10) no feedback: status label is never updated, user does not know whether it worked
        } else if (cmd == "Receipt") {
            // (11) Receipt action is an empty branch / duplicated Pay code: nothing is printed or shown
        } else {
            id.setText(""); amount.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BadFeePaymentWindow().setVisible(true));
    }
}
