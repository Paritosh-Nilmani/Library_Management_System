package util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DBPoolListener implements ServletContextListener {
    @Override public void contextDestroyed(ServletContextEvent event) { DBConnection.shutdown(); }
}
