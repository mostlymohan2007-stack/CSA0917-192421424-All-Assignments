import java.awt.event.*;
import javax.swing.AbstractAction;

/**
 * Task 5 extra comparison: (a) a METHOD REFERENCE and (b) a shared Swing Action object.
 * An Action carries the text, enabled state and handler together, so one object can serve a button, a menu item and a key binding.
 */
public class ConverterMethodRef extends ConverterBase {

    public ConverterMethodRef() { super("Converter - method reference + shared Action"); finishSetup(); }

    private void onConvert(ActionEvent e) { doConversion(); }          // signature matches ActionListener.actionPerformed

    // WIRING-START
    @Override
    protected void wire() {
        btnConvert.addActionListener(this::onConvert);                  // (a) method reference
        // (b) shared Action object used by the text field (and could also be used by a menu item)
        AbstractAction convertAction = new AbstractAction("Convert") {
            @Override public void actionPerformed(ActionEvent e) { doConversion(); }
        };
        txtAmount.setAction(convertAction);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }
    // WIRING-END

    public static void main(String[] args) { java.awt.EventQueue.invokeLater(() -> new ConverterMethodRef().setVisible(true)); }
}
