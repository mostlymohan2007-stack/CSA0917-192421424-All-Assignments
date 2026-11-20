import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Task 1 - redesigned window. The window ONLY collects input and displays results.
 * Validation and payment processing live in FeeService; every button has its own Swing Action
 * (own listener object); the slow work runs in a SwingWorker, never on the Event Dispatch Thread.
 */
public class FeePaymentWindow extends JFrame {
    final JTextField txtStudent = new JTextField(12);
    final JTextField txtAmount = new JTextField(12);
    final JComboBox<String> cmbMethod = new JComboBox<>(FeeService.METHODS.toArray(new String[0]));
    final JLabel lblStatus = new JLabel("Enter the student ID and amount.");
    final JProgressBar busy = new JProgressBar();
    final Action payAction = new PayAction();
    final Action receiptAction = new ReceiptAction();
    final Action clearAction = new ClearAction();

    private final FeeService service;
    private Receipt lastReceipt;

    public FeePaymentWindow(FeeService service) {
        super("Student Fee Payment");
        this.service = service;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Student ID"), g);
        g.gridx = 1; form.add(txtStudent, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Amount"), g);
        g.gridx = 1; form.add(txtAmount, g);
        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Method"), g);
        g.gridx = 1; form.add(cmbMethod, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton pay = new JButton(payAction);
        buttons.add(pay);
        buttons.add(new JButton(receiptAction));
        buttons.add(new JButton(clearAction));
        receiptAction.setEnabled(false);          // nothing to print until a payment succeeds
        getRootPane().setDefaultButton(pay);      // Enter key triggers Pay

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        busy.setIndeterminate(true);
        busy.setVisible(false);
        south.add(lblStatus, BorderLayout.CENTER);
        south.add(busy, BorderLayout.SOUTH);

        add(form, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setSize(Math.max(getWidth(), 380), getHeight());
        setLocationRelativeTo(null);
    }

    private void showStatus(String text, boolean error) {
        lblStatus.setForeground(error ? new Color(176, 0, 32) : new Color(0, 110, 40));
        lblStatus.setText(text);
    }

    /** Separate listener object (Action) for "Pay". */
    class PayAction extends AbstractAction {
        PayAction() { super("Pay"); putValue(SHORT_DESCRIPTION, "Validate and process the payment"); }

        @Override public void actionPerformed(ActionEvent e) {
            final String id = txtStudent.getText(), amount = txtAmount.getText();
            final String method = (String) cmbMethod.getSelectedItem();
            setEnabled(false);
            busy.setVisible(true);
            showStatus("Processing payment...", false);
            new SwingWorker<Receipt, Void>() {
                @Override protected Receipt doInBackground() throws Exception {
                    return service.pay(id, amount, method);              // validation + database: off the EDT
                }
                @Override protected void done() {                          // back on the EDT
                    busy.setVisible(false);
                    setEnabled(true);
                    try {
                        lastReceipt = get();
                        receiptAction.setEnabled(true);
                        showStatus("Payment accepted. Receipt no " + lastReceipt.getNumber() + ".", false);
                    } catch (java.util.concurrent.ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof ValidationException) showStatus(cause.getMessage(), true);
                        else showStatus("Payment failed: the service is not available. Please try again.", true);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }
    }

    /** Separate listener object (Action) for "Receipt". */
    class ReceiptAction extends AbstractAction {
        ReceiptAction() { super("Receipt"); }
        @Override public void actionPerformed(ActionEvent e) {
            if (lastReceipt == null) return;
            JTextArea text = new JTextArea(lastReceipt.toText());
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JOptionPane.showMessageDialog(FeePaymentWindow.this, text, "Receipt", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Separate listener object (Action) for "Clear". */
    class ClearAction extends AbstractAction {
        ClearAction() { super("Clear"); }
        @Override public void actionPerformed(ActionEvent e) {
            txtStudent.setText("");
            txtAmount.setText("");
            cmbMethod.setSelectedIndex(0);
            showStatus("Enter the student ID and amount.", false);
            txtStudent.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        FeeService service = new FeeService(new InMemoryPaymentRepository(400));
        SwingUtilities.invokeLater(() -> new FeePaymentWindow(service).setVisible(true));
    }
}
