import java.awt.event.*;

/** Style 1: the FRAME itself implements ActionListener (one actionPerformed for every source that registers it). */
public class ConverterFrameListener extends ConverterBase implements ActionListener {

    public ConverterFrameListener() { super("Converter - frame implements ActionListener"); finishSetup(); }

    // WIRING-START
    @Override
    protected void wire() {
        btnConvert.addActionListener(this);
        txtAmount.addActionListener(this);          // Enter key in the field
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doConversion();
    }
    // WIRING-END

    public static void main(String[] args) { java.awt.EventQueue.invokeLater(() -> new ConverterFrameListener().setVisible(true)); }
}
