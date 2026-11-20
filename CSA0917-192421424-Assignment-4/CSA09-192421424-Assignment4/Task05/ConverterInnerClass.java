import java.awt.event.*;

/** Style 2: a NAMED inner class implements ActionListener (reusable, can access the frame's fields directly). */
public class ConverterInnerClass extends ConverterBase {

    public ConverterInnerClass() { super("Converter - named inner class"); finishSetup(); }

    // WIRING-START
    private class ConvertListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            doConversion();
        }
    }

    private class CloseHandler extends WindowAdapter {
        @Override public void windowClosing(WindowEvent e) { dispose(); }
    }

    @Override
    protected void wire() {
        ConvertListener listener = new ConvertListener();   // one object, registered twice
        btnConvert.addActionListener(listener);
        txtAmount.addActionListener(listener);              // Enter key in the field
        addWindowListener(new CloseHandler());
    }
    // WIRING-END

    public static void main(String[] args) { java.awt.EventQueue.invokeLater(() -> new ConverterInnerClass().setVisible(true)); }
}
