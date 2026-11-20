package campus.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connection factory. Settings are read, in this order, from: JVM system properties (-Ddb.url=...), the file
 * db.properties in the working folder, the environment variable DB_PASSWORD, and finally safe defaults.
 * No password is written in the source code.
 */
public final class Db {
    private final String url, user, password;

    public Db() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) { p.load(in); } catch (IOException ignored) { /* optional file */ }
        this.url = System.getProperty("db.url", p.getProperty("db.url", "jdbc:mysql://localhost:3306/campus_events"));
        this.user = System.getProperty("db.user", p.getProperty("db.user", "root"));
        String env = System.getenv("DB_PASSWORD");
        this.password = System.getProperty("db.password", p.getProperty("db.password", env == null ? "" : env));
    }

    public Connection get() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String describe() { return url + " (user " + user + ")"; }
}
