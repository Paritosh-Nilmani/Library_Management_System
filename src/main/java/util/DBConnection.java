package util;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/** Shared, bounded connection pool. Credentials come from environment or ignored local config. */
public final class DBConnection {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnection.class);
    private static final Properties PROPERTIES = loadProperties();
    private static final BasicDataSource DATA_SOURCE = createDataSource();

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void shutdown() {
        try {
            DATA_SOURCE.close();
        } catch (SQLException exception) {
            LOG.warn("Unable to close JDBC connection pool cleanly", exception);
        }
    }

    private static BasicDataSource createDataSource() {
        BasicDataSource source = new BasicDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        String url = resolve("db.url", "DB_URL", "");
        if (isBlank(url)) {
            String host = resolve("db.host", "DB_HOST", "127.0.0.1");
            int port = integer("db.port", "DB_PORT", 3306);
            String database = resolve("db.name", "DB_NAME", "library_management_system");
            url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?serverTimezone=UTC&sslMode=PREFERRED&allowPublicKeyRetrieval=true";
        }
        source.setUrl(url);
        source.setUsername(resolve("db.username", "DB_USER", "library_app"));
        source.setPassword(resolve("db.password", "DB_PASSWORD", ""));
        int maxTotal = integer("db.pool.maxTotal", "DB_POOL_MAX_TOTAL", 12);
        int maxIdle = Math.min(maxTotal, integer("db.pool.maxIdle", "DB_POOL_MAX_IDLE", 6));
        int minIdle = Math.min(maxIdle, integer("db.pool.minIdle", "DB_POOL_MIN_IDLE", 2));
        source.setMaxTotal(maxTotal);
        source.setMaxIdle(maxIdle);
        source.setMinIdle(minIdle);
        source.setInitialSize(Math.min(2, maxTotal));
        source.setMaxWaitMillis(5000);
        source.setTestOnBorrow(true);
        source.setValidationQuery("SELECT 1");
        source.setDefaultAutoCommit(true);
        return source;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return properties;
    }

    private static String resolve(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (isBlank(value)) value = System.getenv(environment);
        if (isBlank(value)) value = PROPERTIES.getProperty(property);
        return isBlank(value) ? fallback : value.trim();
    }

    private static int integer(String property, String environment, int fallback) {
        String value = resolve(property, environment, Integer.toString(fallback));
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) throw new NumberFormatException("must be positive");
            return parsed;
        } catch (NumberFormatException exception) {
            LOG.warn("Ignoring invalid JDBC pool setting for {}", property);
            return fallback;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}