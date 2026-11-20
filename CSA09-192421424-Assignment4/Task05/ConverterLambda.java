import java.awt.event.*;

/**
 * Style 4: LAMBDA expressions. This is the "Complete the supplied lambda program" part: the assignment PDF does not
 * print the supplied program, so this is a reconstruction with TODO 1 and TODO 2, both completed below.
 */
public class ConverterLambda extends ConverterBase {

    public ConverterLambda() { super("Converter - lambda"); finishSetup(); }

    // WIRING-START
    @Override
    protected void wire() {
        // TODO 1 (completed): Convert button -> lambda that runs the conversion
        btnConvert.addActionListener(e -> doConversion());
        // TODO 2 (completed): Enter key in the amount field -> the same conversion
        txtAmount.addActionListener(e -> doConversion());
        // WindowAdapter is an abstract-style class with many methods, so a lambda cannot replace it (it is not a functional interface)
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }
    // WIRING-END

    public static void main(String[] args) { java.awt.EventQueue.invokeLater(() -> new ConverterLambda().setVisible(true)); }
}
