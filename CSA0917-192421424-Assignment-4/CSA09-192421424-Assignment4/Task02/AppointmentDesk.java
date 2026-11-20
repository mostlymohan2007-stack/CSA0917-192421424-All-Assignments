import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task 2 - Hospital outpatient appointment desk (AWT).
 * Implements the event table of the report:
 *   Choice (department)  -> ItemEvent   -> ItemListener   -> refresh doctor list from the Map
 *   Button "Book"        -> ActionEvent -> ActionListener  -> validate + book
 *   Button "Clear"       -> ActionEvent -> ActionListener  -> clear form
 *   Frame                -> WindowEvent -> WindowListener  -> opened/activated/.../closing/closed
 * Every listener call is logged so the method-call sequence can be traced.
 */
public class AppointmentDesk extends Frame {
    static final List<String> LOG = new ArrayList<>();
    static void log(String s) { LOG.add(s); System.out.println("   event> " + s); }

    /** Doctors are loaded ONCE into this Map (department -> doctors). */
    final Map<String, List<String>> doctorsByDepartment = new LinkedHashMap<>();
    final Map<String, String> bookedSlots = new LinkedHashMap<>();     // "doctor|slot" -> patient

    final TextField txtPatient = new TextField(18);
    final TextField txtPhone = new TextField(12);
    final Choice chDepartment = new Choice();
    final Choice chDoctor = new Choice();
    final Choice chSlot = new Choice();
    final Button btnBook = new Button("Book");
    final Button btnClear = new Button("Clear");
    final Label lblStatus = new Label("Select a department.");

    public AppointmentDesk() {
        super("Outpatient Appointment Desk");
        loadDoctorsOnce();
        buildScreen();
        wireListeners();
    }

    private void loadDoctorsOnce() {
        doctorsByDepartment.put("General Medicine", List.of("Dr. Rao", "Dr. Iyer"));
        doctorsByDepartment.put("Cardiology", List.of("Dr. Mehta", "Dr. Nair", "Dr. Khan"));
        doctorsByDepartment.put("Orthopedics", List.of("Dr. Das"));
        doctorsByDepartment.put("Pediatrics", List.of("Dr. Sen", "Dr. Paul"));
    }

    /** Containment tree: Frame > [Panel(form) > Label/TextField/Choice..., Panel(buttons) > Button, Label(status)] */
    private void buildScreen() {
        setLayout(new BorderLayout(6, 6));
        Panel form = new Panel(new GridLayout(5, 2, 6, 6));
        form.add(new Label("Patient name"));  form.add(txtPatient);
        form.add(new Label("Phone"));         form.add(txtPhone);
        form.add(new Label("Department"));    form.add(chDepartment);
        form.add(new Label("Doctor"));        form.add(chDoctor);
        form.add(new Label("Slot"));          form.add(chSlot);
        chDepartment.add("-- select --");
        for (String d : doctorsByDepartment.keySet()) chDepartment.add(d);
        for (String s : new String[]{"09:00", "09:30", "10:00", "10:30", "11:00"}) chSlot.add(s);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(btnBook); buttons.add(btnClear);
        Panel south = new Panel(new BorderLayout());
        south.add(buttons, BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.SOUTH);

        add(new Label("Outpatient Appointment Desk", Label.CENTER), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setSize(Math.max(getWidth(), 420), getHeight());
    }

    private void wireListeners() {
        chDepartment.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(ItemEvent e) {
                log("ItemListener.itemStateChanged  (department = " + e.getItem() + ")");
                refreshDoctors((String) e.getItem());
            }
        });
        btnBook.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { log("ActionListener.actionPerformed  [Book]"); book(); }
        });
        btnClear.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { log("ActionListener.actionPerformed  [Clear]"); clear(); }
        });
        addWindowListener(new WindowListener() {
            @Override public void windowOpened(WindowEvent e)      { log("WindowListener.windowOpened"); }
            @Override public void windowActivated(WindowEvent e)   { log("WindowListener.windowActivated"); }
            @Override public void windowDeactivated(WindowEvent e) { log("WindowListener.windowDeactivated"); }
            @Override public void windowIconified(WindowEvent e)   { log("WindowListener.windowIconified"); }
            @Override public void windowDeiconified(WindowEvent e) { log("WindowListener.windowDeiconified"); }
            @Override public void windowClosing(WindowEvent e)     { log("WindowListener.windowClosing  -> dispose()"); dispose(); }
            @Override public void windowClosed(WindowEvent e)      { log("WindowListener.windowClosed"); }
        });
    }

    /** Pseudocode 1: refresh the doctor list when the department changes. */
    void refreshDoctors(String department) {
        chDoctor.removeAll();
        List<String> list = doctorsByDepartment.get(department);      // O(1) Map lookup, no database access
        if (list == null) { lblStatus.setText("Select a department."); return; }
        for (String d : list) chDoctor.add(d);
        lblStatus.setText(list.size() + " doctor(s) available in " + department);
    }

    /** Pseudocode 2: validate and book an appointment. */
    void book() {
        String name = txtPatient.getText().trim();
        String phone = txtPhone.getText().trim();
        if (name.isEmpty())                   { lblStatus.setText("Enter the patient name."); return; }
        if (!phone.matches("\\d{10}"))        { lblStatus.setText("Phone must be 10 digits."); return; }
        if (chDepartment.getSelectedIndex() == 0 || chDoctor.getItemCount() == 0)
                                              { lblStatus.setText("Select a department and a doctor."); return; }
        String key = chDoctor.getSelectedItem() + "|" + chSlot.getSelectedItem();
        if (bookedSlots.containsKey(key))     { lblStatus.setText("Slot already taken. Choose another slot."); return; }
        bookedSlots.put(key, name);
        lblStatus.setText("Booked: " + name + " with " + chDoctor.getSelectedItem() + " at " + chSlot.getSelectedItem());
    }

    void clear() {
        txtPatient.setText(""); txtPhone.setText("");
        chDepartment.select(0); chDoctor.removeAll();
        lblStatus.setText("Select a department.");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Task 2: method-call sequence from opening to closing the window ===");
        AppointmentDesk f = new AppointmentDesk();
        f.setLocation(200, 150);
        f.setAlwaysOnTop(true);
        f.setVisible(true);                       // -> windowOpened, windowActivated
        Thread.sleep(800);

        EventQueue.invokeAndWait(() -> {
            f.txtPatient.setText("Anita Rao"); f.txtPhone.setText("9876543210");
            f.chDepartment.select("Cardiology");
            f.chDepartment.dispatchEvent(new ItemEvent(f.chDepartment, ItemEvent.ITEM_STATE_CHANGED, "Cardiology", ItemEvent.SELECTED));
        });
        Thread.sleep(300);
        Shot.save(f, "output/task02_desk_department_selected.png");
        EventQueue.invokeAndWait(() -> f.btnBook.dispatchEvent(new ActionEvent(f.btnBook, ActionEvent.ACTION_PERFORMED, "Book")));
        Thread.sleep(300);
        System.out.println("   status after valid booking : " + f.lblStatus.getText());
        Shot.save(f, "output/task02_desk_booked.png");
        EventQueue.invokeAndWait(() -> f.btnBook.dispatchEvent(new ActionEvent(f.btnBook, ActionEvent.ACTION_PERFORMED, "Book")));
        System.out.println("   status after the same slot : " + f.lblStatus.getText());
        EventQueue.invokeAndWait(() -> { f.txtPhone.setText("12ab"); f.btnBook.dispatchEvent(new ActionEvent(f.btnBook, ActionEvent.ACTION_PERFORMED, "Book")); });
        System.out.println("   status after bad phone     : " + f.lblStatus.getText());

        EventQueue.invokeAndWait(() -> f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING)));   // user clicks X
        Thread.sleep(500);
        System.out.println();
        System.out.println("Recorded sequence (" + LOG.size() + " events):");
        for (int i = 0; i < LOG.size(); i++) System.out.printf("  %2d. %s%n", i + 1, LOG.get(i));
        System.exit(0);
    }
}
