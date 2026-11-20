import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Task 3 - resizing test. Opens the screen at three window sizes, records the height/width of each region
 * and takes a screenshot at each size. Expected: only the results list grows; the search row and the
 * bottom button row keep their preferred height.
 */
public class LibraryLayoutTest {
    static void line(String label, Component c) {
        System.out.printf("   %-22s width=%4d  height=%4d%n", label, c.getWidth(), c.getHeight());
    }

    public static void main(String[] args) throws Exception {
        final LibrarySearch[] h = new LibrarySearch[1];
        EventQueue.invokeAndWait(() -> { h[0] = new LibrarySearch(); h[0].setAlwaysOnTop(true); h[0].setLocation(120, 80); h[0].setSize(520, 380); h[0].setVisible(true); });
        LibrarySearch f = h[0];
        Thread.sleep(700);

        EventQueue.invokeAndWait(() -> { f.txtSearch.setText("java"); f.btnSearch.dispatchEvent(new ActionEvent(f.btnSearch, ActionEvent.ACTION_PERFORMED, "Search")); });
        Thread.sleep(300);
        System.out.println("Search 'java' (All): status = " + f.lblStatus.getText() + " | " + f.lblPage.getText());

        int[][] sizes = {{520, 380}, {800, 560}, {1200, 720}};
        String[] names = {"small", "medium", "large"};
        for (int i = 0; i < sizes.length; i++) {
            final int w = sizes[i][0], hh = sizes[i][1];
            EventQueue.invokeAndWait(() -> { f.setSize(w, hh); f.validate(); });
            Thread.sleep(600);
            System.out.println("Window size " + w + " x " + hh + " (" + names[i] + "):");
            line("title (top)", f.lblTitle);
            line("search row", f.searchRow);
            line("results list (centre)", f.listResults);
            line("bottom button row", f.buttonRow);
            line("status label", f.lblStatus);
            Shot.save(f, "output/task03_size_" + (i + 1) + "_" + names[i] + ".png");
        }

        // Interaction tests at the large size: an empty search lists the whole catalogue (26 books = 4 pages)
        EventQueue.invokeAndWait(() -> { f.txtSearch.setText(""); f.btnSearch.dispatchEvent(new ActionEvent(f.btnSearch, ActionEvent.ACTION_PERFORMED, "Search")); });
        Thread.sleep(300);
        System.out.println("Search '' (All): status = " + f.lblStatus.getText() + " | " + f.lblPage.getText());
        EventQueue.invokeAndWait(() -> { f.listResults.select(1); f.btnReserve.dispatchEvent(new ActionEvent(f.btnReserve, ActionEvent.ACTION_PERFORMED, "Reserve")); });
        Thread.sleep(300);
        System.out.println("Reserve selected book: " + f.lblStatus.getText());
        EventQueue.invokeAndWait(() -> f.btnNext.dispatchEvent(new ActionEvent(f.btnNext, ActionEvent.ACTION_PERFORMED, "Next")));
        Thread.sleep(300);
        System.out.println("Next page: " + f.lblPage.getText());
        Shot.save(f, "output/task03_next_page_after_reserve.png");
        EventQueue.invokeAndWait(() -> { f.txtSearch.setText("zzzz"); f.btnSearch.dispatchEvent(new ActionEvent(f.btnSearch, ActionEvent.ACTION_PERFORMED, "Search")); });
        Thread.sleep(300);
        System.out.println("No-result search: " + f.lblStatus.getText());
        EventQueue.invokeAndWait(f::dispose);
        System.exit(0);
    }
}
