import java.awt.event.*;

/** Style 3: ANONYMOUS inner classes - the handler is written exactly where it is registered. */
public class ConverterAnonymous extends ConverterBase {

    public ConverterAnonymous() { super("Converter - anonymous inner class"); finishSetup(); }

    // WIRING-START
    @Override
    protected void wire() {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doConversion();
            }
        };
        btnConvert.addActionListener(listener);
        txtAmount.addActionListener(listener);              // Enter key in the field
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }
    // WIRING-END

    public static void main(String[] args) { java.awt.EventQueue.invokeLater(() -> new ConverterAnonymous().setVisible(true)); }
}
