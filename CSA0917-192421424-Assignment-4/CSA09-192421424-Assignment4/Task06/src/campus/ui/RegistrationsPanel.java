package campus.ui;

import campus.model.*;
import campus.service.RegistrationService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Screen 2: committee view - table of registrations, filter by event, paged loading with SwingWorker. */
public class RegistrationsPanel extends JPanel {
    static final int PAGE_SIZE = 25;
    static final String ALL = "All events";

    final JComboBox<Object> cmbFilter = new JComboBox<>();
    final JButton btnRefresh = new JButton("Refresh");
    final JButton btnPrev = new JButton("< Prev");
    final JButton btnNext = new JButton("Next >");
    final JLabel lblPage = new JLabel("Page 0 of 0");
    final JLabel lblStatus = new JLabel(" ");
    final JProgressBar busy = new JProgressBar();
    final RowsModel model = new RowsModel();
    final JTable table = new JTable(model);

    private final RegistrationService service;
    private int page = 0;
    private boolean updatingFilter = false;

    public RegistrationsPanel(RegistrationService service) {
        this.service = service;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Event:"));
        cmbFilter.addItem(ALL);
        top.add(cmbFilter);
        top.add(btnRefresh);

        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pager.add(btnPrev); pager.add(lblPage); pager.add(btnNext);
        busy.setIndeterminate(true); busy.setVisible(false);
        bottom.add(pager, BorderLayout.NORTH);
        bottom.add(lblStatus, BorderLayout.CENTER);
        bottom.add(busy, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        cmbFilter.addActionListener(e -> { if (!updatingFilter) reload(0); });
        btnRefresh.addActionListener(e -> reload(page));
        btnPrev.addActionListener(e -> reload(page - 1));
        btnNext.addActionListener(e -> reload(page + 1));
    }

    void setEvents(List<EventInfo> events) {
        updatingFilter = true;
        cmbFilter.removeAllItems();
        cmbFilter.addItem(ALL);
        for (EventInfo ev : events) cmbFilter.addItem(ev);
        updatingFilter = false;
    }

    Integer selectedEventId() {
        Object o = cmbFilter.getSelectedItem();
        return (o instanceof EventInfo) ? ((EventInfo) o).getEventId() : null;
    }

    /** Pseudocode "Filter": disable controls, load ONE page in a SwingWorker, then fill the table on the EDT. */
    void reload(int wantedPage) {
        final Integer eventId = selectedEventId();
        setBusy(true);
        new SwingWorker<RegistrationService.Page, Void>() {
            @Override protected RegistrationService.Page doInBackground() throws Exception {
                return service.loadPage(eventId, wantedPage, PAGE_SIZE);      // COUNT + SELECT ... LIMIT ? OFFSET ?
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    RegistrationService.Page p = get();
                    page = p.page;
                    model.setRows(p.rows);
                    lblPage.setText("Page " + (p.page + 1) + " of " + p.pages());
                    btnPrev.setEnabled(p.page > 0);
                    btnNext.setEnabled(p.page + 1 < p.pages());
                    String who = eventId == null ? "all events" : String.valueOf(cmbFilter.getSelectedItem());
                    lblStatus.setForeground(new Color(0, 110, 40));
                    lblStatus.setText(p.total + " registration(s) for " + who + "; showing " + p.rows.size() + " row(s).");
                } catch (ExecutionException ex) {
                    lblStatus.setForeground(new Color(176, 0, 32));
                    lblStatus.setText("Could not load registrations: " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setBusy(boolean b) {
        busy.setVisible(b);
        cmbFilter.setEnabled(!b); btnRefresh.setEnabled(!b);
        if (b) { btnPrev.setEnabled(false); btnNext.setEnabled(false); }
    }

    int currentPage() { return page; }

    /** Table model: shows exactly one page of rows. */
    static final class RowsModel extends AbstractTableModel {
        private static final String[] COLS = {"Reg #", "Student ID", "Name", "Event", "Registered at"};
        private List<RegistrationRow> rows = new ArrayList<>();
        void setRows(List<RegistrationRow> r) { rows = r; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Object getValueAt(int r, int c) {
            RegistrationRow x = rows.get(r);
            switch (c) {
                case 0: return x.regId;
                case 1: return x.studentId;
                case 2: return x.studentName;
                case 3: return x.eventTitle;
                default: return x.registeredAt;
            }
        }
    }
}
