import java.awt.*;
import javax.swing.*;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Task 5 - the screen and the conversion logic, shared by the four versions.
 * Only the way the Convert action is CONNECTED (the wire() method of each subclass) differs between versions.
 * Exchange rates are fixed sample rates (units of currency per 1 USD), not live market data.
 */
public abstract class ConverterBase extends JFrame {
    static final Map<String, Double> PER_USD = new LinkedHashMap<>();
    static {
        PER_USD.put("USD", 1.0);
        PER_USD.put("INR", 83.0);
        PER_USD.put("EUR", 0.92);
        PER_USD.put("GBP", 0.79);
        PER_USD.put("JPY", 150.0);
    }

    protected final JTextField txtAmount = new JTextField(10);
    protected final JComboBox<String> cmbFrom = new JComboBox<>(PER_USD.keySet().toArray(new String[0]));
    protected final JComboBox<String> cmbTo = new JComboBox<>(PER_USD.keySet().toArray(new String[0]));
    protected final JButton btnConvert = new JButton("Convert");
    protected final JLabel lblResult = new JLabel("Result will appear here");

    protected ConverterBase(String title) {
        super(title);
        cmbTo.setSelectedItem("INR");
        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        form.add(new JLabel("Amount"));  form.add(txtAmount);
        form.add(new JLabel("From"));    form.add(cmbFrom);
        form.add(new JLabel("To"));      form.add(cmbTo);
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        south.add(btnConvert, BorderLayout.NORTH);
        south.add(lblResult, BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);   // closing is handled by a WindowAdapter in wire()
        setLocation(200, 150);
    }

    /** Called by subclasses after construction: connect the listeners. */
    protected abstract void wire();

    protected final void finishSetup() {
        wire();
        pack();
        setSize(Math.max(getWidth(), 360), getHeight());
    }

    /** Pure conversion logic (testable without a window). */
    static double convert(double amount, String from, String to) {
        return amount / PER_USD.get(from) * PER_USD.get(to);
    }

    /** What every version runs when Convert is pressed or Enter is typed in the amount field. */
    protected final void doConversion() {
        String text = txtAmount.getText().trim();
        try {
            double amount = Double.parseDouble(text);
            if (amount < 0) throw new NumberFormatException("negative");
            String from = (String) cmbFrom.getSelectedItem(), to = (String) cmbTo.getSelectedItem();
            String result = new DecimalFormat("#,##0.00").format(convert(amount, from, to));
            lblResult.setForeground(new Color(0, 110, 40));
            lblResult.setText(new DecimalFormat("#,##0.00").format(amount) + " " + from + " = " + result + " " + to);
            JOptionPane.showMessageDialog(this, lblResult.getText(), "Conversion result", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            lblResult.setForeground(new Color(176, 0, 32));
            lblResult.setText("Invalid amount: enter a non-negative number.");
            JOptionPane.showMessageDialog(this, "\"" + text + "\" is not a valid amount.\nEnter a number such as 100 or 2500.50.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        }
    }
}
