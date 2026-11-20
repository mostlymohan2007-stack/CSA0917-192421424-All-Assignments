package campus.ui;

import campus.model.*;
import campus.service.RegistrationService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Screen 1: student registration form. Collects input and shows results; all rules are in RegistrationService. */
public class RegisterPanel extends JPanel {
    final JTextField txtId = new JTextField(12);
    final JTextField txtName = new JTextField(24);
    final JTextField txtEmail = new JTextField(24);
    final JComboBox<String> cmbDept = new JComboBox<>(RegistrationService.DEPARTMENTS.toArray(new String[0]));
    final JComboBox<EventInfo> cmbEvent = new JComboBox<>();
    final JButton btnRegister = new JButton("Register");
    final JButton btnClear = new JButton("Clear");
    final JLabel lblStatus = new JLabel("Fill in the form and press Register.");
    final JProgressBar busy = new JProgressBar();

    private final RegistrationService service;
    private final Runnable onRegistered;

    public RegisterPanel(RegistrationService service, Runnable onRegistered) {
        this.service = service;
        this.onRegistered = onRegistered;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Student ID", "Full name", "E-mail", "Department", "Event"};
        JComponent[] fields = {txtId, txtName, txtEmail, cmbDept, cmbEvent};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.fill = GridBagConstraints.NONE; g.weightx = 0;
            form.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
            form.add(fields[i], g);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(btnRegister); buttons.add(btnClear);
        busy.setIndeterminate(true); busy.setVisible(false);
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(buttons, BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.CENTER);
        south.add(busy, BorderLayout.SOUTH);

        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> submit());
        btnClear.addActionListener(e -> clear());
        txtEmail.addActionListener(e -> submit());
    }

    void setEvents(List<EventInfo> events) {
        cmbEvent.removeAllItems();
        for (EventInfo ev : events) cmbEvent.addItem(ev);
    }

    void clear() {
        txtId.setText(""); txtName.setText(""); txtEmail.setText("");
        cmbDept.setSelectedIndex(0);
        status("Form cleared.", false);
        txtId.requestFocusInWindow();
    }

    private void status(String text, boolean error) {
        lblStatus.setForeground(error ? new Color(176, 0, 32) : new Color(0, 110, 40));
        lblStatus.setText(text);
    }

    /** Runs on the EDT (button click). The database work runs in a SwingWorker thread. */
    void submit() {
        final String id = txtId.getText(), name = txtName.getText(), email = txtEmail.getText();
        final String dept = (String) cmbDept.getSelectedItem();
        final EventInfo event = (EventInfo) cmbEvent.getSelectedItem();
        btnRegister.setEnabled(false);
        busy.setVisible(true);
        status("Registering...", false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.register(id, name, email, dept, event);       // validation + duplicate check + JDBC: NOT on the EDT
                return null;
            }
            @Override protected void done() {                          // back on the EDT
                busy.setVisible(false);
                btnRegister.setEnabled(true);
                try {
                    get();
                    status("Registered " + id.trim().toUpperCase() + " for \"" + event + "\".", false);
                    onRegistered.run();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    if (c instanceof RegistrationException) status(c.getMessage(), true);          // validation / duplicate / full
                    else status("Database problem: please try again later. (" + c.getClass().getSimpleName() + ")", true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
