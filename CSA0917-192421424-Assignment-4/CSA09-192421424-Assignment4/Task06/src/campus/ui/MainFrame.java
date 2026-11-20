package campus.ui;

import campus.model.EventInfo;
import campus.service.RegistrationService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Main window: menu bar + two screens (tabs).
 *   File -> Export registrations to CSV... | Exit      View -> Register | Registrations      Help -> About
 */
public class MainFrame extends JFrame {
    final RegisterPanel registerPanel;
    final RegistrationsPanel registrationsPanel;
    final JTabbedPane tabs = new JTabbedPane();
    final JMenuItem miExport = new JMenuItem("Export registrations to CSV...");
    private final RegistrationService service;

    public MainFrame(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        registrationsPanel = new RegistrationsPanel(service);
        registerPanel = new RegisterPanel(service, () -> registrationsPanel.reload(registrationsPanel.currentPage()));
        tabs.addTab("Register", registerPanel);
        tabs.addTab("Registrations", registrationsPanel);
        add(tabs, BorderLayout.CENTER);
        setJMenuBar(buildMenu());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        setSize(760, 520);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        miExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        miExport.addActionListener(e -> chooseAndExport());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        file.add(miExport); file.addSeparator(); file.add(exit);

        JMenu view = new JMenu("View");
        JMenuItem v1 = new JMenuItem("Register"), v2 = new JMenuItem("Registrations");
        v1.addActionListener(e -> tabs.setSelectedIndex(0));
        v2.addActionListener(e -> { tabs.setSelectedIndex(1); registrationsPanel.reload(0); });
        view.add(v1); view.add(v2);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nSwing + JDBC + MySQL (CSA09 Assignment 4, Task 6)", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(file); bar.add(view); bar.add(help);
        return bar;
    }

    /** Loads the events once (background) and fills both combo boxes, then shows the first page. */
    public void start() {
        new SwingWorker<List<EventInfo>, Void>() {
            @Override protected List<EventInfo> doInBackground() throws Exception { return service.events(); }
            @Override protected void done() {
                try {
                    List<EventInfo> ev = get();
                    registerPanel.setEvents(ev);
                    registrationsPanel.setEvents(ev);
                    registrationsPanel.reload(0);
                } catch (ExecutionException ex) {
                    registerPanel.lblStatus.setText("Cannot connect to the database: " + ex.getCause().getMessage());
                    registerPanel.lblStatus.setForeground(new Color(176, 0, 32));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void chooseAndExport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("registrations.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) exportTo(fc.getSelectedFile().toPath());
    }

    /** Export in the background; the list currently filtered in the Registrations tab is exported. */
    void exportTo(Path file) {
        final Integer eventId = registrationsPanel.selectedEventId();
        registrationsPanel.busy.setVisible(true);
        registrationsPanel.lblStatus.setText("Exporting...");
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return service.exportCsv(eventId, file); }
            @Override protected void done() {
                registrationsPanel.busy.setVisible(false);
                try {
                    registrationsPanel.lblStatus.setForeground(new Color(0, 110, 40));
                    registrationsPanel.lblStatus.setText("Exported " + get() + " registration(s) to " + file.getFileName());
                } catch (ExecutionException ex) {
                    registrationsPanel.lblStatus.setForeground(new Color(176, 0, 32));
                    registrationsPanel.lblStatus.setText("Export failed: " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
