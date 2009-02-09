// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.cwm.db.connection;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;
import net.sourceforge.sqlexplorer.util.MyURLClassLoader;

import org.apache.log4j.Logger;
import org.talend.utils.sugars.ReturnCode;

/**
 * Utility class for database connection handling.
 */
public final class ConnectionUtils {

    private static Logger log = Logger.getLogger(ConnectionUtils.class);

    // MOD xqliu 2009-02-02 bug 5261
    public static final int LOGIN_TEMEOUT_MILLISECOND = 20000;

    public static final int LOGIN_TIMEOUT_SECOND = 20;

    private static boolean timeout = true;

    public static boolean isTimeout() {
        return timeout;
    }

    public static void setTimeout(boolean timeout) {
        ConnectionUtils.timeout = timeout;
    }

    /**
     * The query to execute in order to verify the connection.
     */
    // private static final String PING_SELECT = "SELECT 1";
    /**
     * private constructor.
     */
    private ConnectionUtils() {
    }

    /**
     * Method "createConnection".
     * 
     * @param url the database url
     * @param driverClassName the Driver classname
     * @param props properties passed to the driver manager for getting the connection (normally at least a "user" and
     * "password" property should be included)
     * @return the connection
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Connection createConnection(String url, String driverClassName, Properties props) throws SQLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException {
        Driver driver = getClassDriver(driverClassName);
        if (driver != null) {
            DriverManager.registerDriver(driver);
            if (log.isDebugEnabled()) {
                log.debug("SQL driver found and registered: " + driverClassName);
                log.debug("Enumerating all drivers:");
                Enumeration<Driver> drivers = DriverManager.getDrivers();
                while (drivers.hasMoreElements()) {
                    log.debug(drivers.nextElement());
                }
            }
            Connection connection = null;
            if (driverClassName.equals("org.hsqldb.jdbcDriver")) {
                // MOD xqliu 2009-02-02 bug 5261
                if (isTimeout()) {
                    DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECOND);
                }
                connection = DriverManager.getConnection(url, props);
            } else {
                // MOD xqliu 2009-02-02 bug 5261
                connection = createConnectionWithTimeout(driver, url, props);
            }

            return connection;
        }
        return null;

    }

    /**
     * 
     * DOC xqliu Comment method "createConnectionWithTimeout".
     * 
     * @param driver
     * @param url
     * @param props
     * @return
     * @throws SQLException
     */
    public static Connection createConnectionWithTimeout(Driver driver, String url, Properties props) throws SQLException {
        Connection ret = null;
        if (isTimeout()) {
            ConnectionCreator cc = new ConnectionCreator(driver, url, props);
            new Thread(cc).start();
            long begin = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - begin > LOGIN_TEMEOUT_MILLISECOND) {
                    break;
                }
                if (cc.getConnection() != null) {
                    ret = cc.getConnection();
                    break;
                }
                if (cc.getExecption() != null) {
                    throw cc.getExecption();
                }
            }
            cc = null;
            if (ret == null) {
                throw new SQLException("Connection Timeout!");
            }
        } else {
            ret = driver.connect(url, props);
        }
        return ret;
    }

    /**
     * DOC qzhang Comment method "getClassDriver".
     * 
     * @param driverClassName
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static Driver getClassDriver(String driverClassName) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        SQLExplorerPlugin sqlExplorerPlugin = SQLExplorerPlugin.getDefault();
        Driver driver = null;
        if (sqlExplorerPlugin != null) {
            net.sourceforge.sqlexplorer.dbproduct.DriverManager driverModel = sqlExplorerPlugin.getDriverModel();
            try {
                Collection<ManagedDriver> drivers = driverModel.getDrivers();
                for (ManagedDriver managedDriver : drivers) {
                    LinkedList<String> jars = managedDriver.getJars();
                    List<URL> urls = new ArrayList<URL>();
                    for (int i = 0; i < jars.size(); i++) {
                        File file = new File(jars.get(i));
                        if (file.exists()) {
                            urls.add(file.toURL());
                        }
                    }
                    if (!urls.isEmpty()) {
                        try {
                            MyURLClassLoader cl;
                            cl = new MyURLClassLoader(urls.toArray(new URL[0]));
                            Class clazz = cl.findClass(driverClassName);
                            if (clazz != null) {
                                driver = (Driver) clazz.newInstance();
                                return driver; // driver is found
                            }
                        } catch (ClassNotFoundException e) {
                            // do nothings
                        }
                    }

                }
            } catch (MalformedURLException e) {
                // do nothings
            }
        }
        if (driver == null) {
            driver = (Driver) Class.forName(driverClassName).newInstance();
        }
        return driver;
    }

    /**
     * Method "isValid".
     * 
     * @param connection the connection to test
     * @return a return code with the appropriate message (never null)
     */
    public static ReturnCode isValid(final Connection connection) {
        return org.talend.utils.sql.ConnectionUtils.isValid(connection);
    }

    /**
     * Method "closeConnection".
     * 
     * @param connection the connection to close.
     * @return a ReturnCode with true if ok, false if problem. {@link ReturnCode#getMessage()} gives the error message
     * when there is a problem.
     */
    public static ReturnCode closeConnection(final Connection connection) {
        return org.talend.utils.sql.ConnectionUtils.closeConnection(connection);
    }
}
