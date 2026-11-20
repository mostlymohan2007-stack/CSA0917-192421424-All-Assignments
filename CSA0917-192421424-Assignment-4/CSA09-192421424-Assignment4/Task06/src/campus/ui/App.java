package campus.ui;

import campus.dao.Db;
import campus.dao.JdbcRegistrationDao;
import campus.service.RegistrationService;

import javax.swing.*;

/** Entry point: wires the three layers (UI -> service -> JDBC DAO) and shows the window. */
public class App {
    public static void main(String[] args) {
        Db db = new Db();
        RegistrationService service = new RegistrationService(new JdbcRegistrationDao(db));
        SwingUtilities.invokeLater(() -> {
            MainFrame f = new MainFrame(service);
            f.setVisible(true);
            f.start();
        });
    }
}
