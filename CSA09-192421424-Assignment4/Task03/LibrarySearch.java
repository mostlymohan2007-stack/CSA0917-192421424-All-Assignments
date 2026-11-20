import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Task 3 - Campus library search screen (AWT).
 *
 * Nested panel structure and layout managers:
 *   Frame                       BorderLayout
 *   +- NORTH  Panel "top"       BorderLayout
 *   |    +- NORTH  Label        title (centred)
 *   |    +- CENTER Panel        FlowLayout(LEFT)   Label, TextField, Choice, Button "Search"
 *   +- CENTER  List             results (the only region that grows)
 *   +- SOUTH  Panel "bottom"    BorderLayout
 *        +- NORTH  Panel        FlowLayout(CENTER) Button Prev, Button Next, Button Reserve, Label page
 *        +- SOUTH  Label        status message
 */
public class LibrarySearch extends Frame {
    static final int PAGE_SIZE = 8;

    static final String[][] CATALOG = {
        {"Programming in Java", "Herbert Schildt", "Computer Science"},
        {"Java: The Complete Reference", "Herbert Schildt", "Computer Science"},
        {"Effective Java", "Joshua Bloch", "Computer Science"},
        {"Head First Java", "Kathy Sierra", "Computer Science"},
        {"Core Java Volume I", "Cay Horstmann", "Computer Science"},
        {"Data Structures Using Java", "Yedidyah Langsam", "Computer Science"},
        {"Introduction to Algorithms", "Thomas Cormen", "Computer Science"},
        {"Operating System Concepts", "Abraham Silberschatz", "Computer Science"},
        {"Database System Concepts", "Abraham Silberschatz", "Computer Science"},
        {"Computer Networks", "Andrew Tanenbaum", "Computer Science"},
        {"Engineering Mathematics", "B. S. Grewal", "Mathematics"},
        {"Linear Algebra and Its Applications", "Gilbert Strang", "Mathematics"},
        {"Discrete Mathematics", "Kenneth Rosen", "Mathematics"},
        {"Probability and Statistics", "Morris DeGroot", "Mathematics"},
        {"Concepts of Physics", "H. C. Verma", "Physics"},
        {"Fundamentals of Physics", "David Halliday", "Physics"},
        {"Organic Chemistry", "Morrison and Boyd", "Chemistry"},
        {"Physical Chemistry", "Peter Atkins", "Chemistry"},
        {"The Discovery of India", "Jawaharlal Nehru", "History"},
        {"India After Gandhi", "Ramachandra Guha", "History"},
        {"Wings of Fire", "A. P. J. Abdul Kalam", "Biography"},
        {"The Story of My Experiments with Truth", "M. K. Gandhi", "Biography"},
        {"Clean Code", "Robert Martin", "Computer Science"},
        {"Java Concurrency in Practice", "Brian Goetz", "Computer Science"},
        {"Python Crash Course", "Eric Matthes", "Computer Science"},
        {"Digital Design", "Morris Mano", "Electronics"},
    };

    final Label lblTitle = new Label("Campus Library - Book Search", Label.CENTER);
    final TextField txtSearch = new TextField(24);
    final Choice chCategory = new Choice();
    final Button btnSearch = new Button("Search");
    final List listResults = new List(10, false);
    final Button btnPrev = new Button("Prev");
    final Button btnNext = new Button("Next");
    final Button btnReserve = new Button("Reserve");
    final Label lblPage = new Label("Page 0 of 0");
    final Label lblStatus = new Label("Type a word and press Search.");
    final Panel top = new Panel(new BorderLayout());
    final Panel searchRow = new Panel(new FlowLayout(FlowLayout.LEFT));
    final Panel bottom = new Panel(new BorderLayout());
    final Panel buttonRow = new Panel(new FlowLayout(FlowLayout.CENTER));

    private final java.util.List<String[]> matches = new ArrayList<>();
    private final Set<String> reserved = new HashSet<>();
    private int page = 0;

    public LibrarySearch() {
        super("Campus Library Search");
        buildLayout();
        wire();
        showPage();
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        top.add(lblTitle, BorderLayout.NORTH);
        for (String c : new String[]{"All", "Title", "Author", "Subject"}) chCategory.add(c);
        searchRow.add(new Label("Search:"));
        searchRow.add(txtSearch);
        searchRow.add(chCategory);
        searchRow.add(btnSearch);
        top.add(searchRow, BorderLayout.CENTER);

        buttonRow.add(btnPrev); buttonRow.add(btnNext); buttonRow.add(btnReserve); buttonRow.add(lblPage);
        bottom.add(buttonRow, BorderLayout.NORTH);
        bottom.add(lblStatus, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(listResults, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        pack();
    }

    private void wire() {
        btnSearch.addActionListener(e -> search());
        txtSearch.addActionListener(e -> search());
        btnPrev.addActionListener(e -> { if (page > 0) { page--; showPage(); } });
        btnNext.addActionListener(e -> { if ((page + 1) * PAGE_SIZE < matches.size()) { page++; showPage(); } });
        btnReserve.addActionListener(e -> reserve());
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
    }

    void search() {
        String q = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String cat = chCategory.getSelectedItem();
        matches.clear();
        for (String[] b : CATALOG) {
            boolean hit = switch (cat) {
                case "Title" -> b[0].toLowerCase(Locale.ROOT).contains(q);
                case "Author" -> b[1].toLowerCase(Locale.ROOT).contains(q);
                case "Subject" -> b[2].toLowerCase(Locale.ROOT).contains(q);
                default -> (b[0] + " " + b[1] + " " + b[2]).toLowerCase(Locale.ROOT).contains(q);
            };
            if (hit) matches.add(b);
        }
        page = 0;
        showPage();
        lblStatus.setText(matches.isEmpty() ? "No books found for \"" + q + "\" in " + cat + "."
                : matches.size() + " book(s) found for \"" + q + "\" in " + cat + ".");
    }

    private void showPage() {
        listResults.removeAll();
        int from = page * PAGE_SIZE, to = Math.min(from + PAGE_SIZE, matches.size());
        for (int i = from; i < to; i++) {
            String[] b = matches.get(i);
            listResults.add(b[0] + "  -  " + b[1] + "  [" + b[2] + "]" + (reserved.contains(b[0]) ? "  (reserved)" : ""));
        }
        int pages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        lblPage.setText("Page " + (matches.isEmpty() ? 0 : page + 1) + " of " + (matches.isEmpty() ? 0 : pages));
        btnPrev.setEnabled(page > 0);
        btnNext.setEnabled((page + 1) * PAGE_SIZE < matches.size());
        btnReserve.setEnabled(!matches.isEmpty());
    }

    void reserve() {
        int idx = listResults.getSelectedIndex();
        if (idx < 0) { lblStatus.setText("Select a book in the list first."); return; }
        String[] b = matches.get(page * PAGE_SIZE + idx);
        if (!reserved.add(b[0])) { lblStatus.setText("\"" + b[0] + "\" is already reserved."); return; }
        lblStatus.setText("Reserved: " + b[0]);
        showPage();
    }

    public static void main(String[] args) {
        LibrarySearch f = new LibrarySearch();
        f.setSize(760, 520);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
