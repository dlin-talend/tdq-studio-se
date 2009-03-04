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
package org.talend.dq.connection;

import java.sql.SQLException;
import java.util.LinkedList;

import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;

import org.apache.log4j.Logger;
import org.talend.cwm.db.connection.DBConnect;
import org.talend.cwm.db.connection.TalendCwmFactory;
import org.talend.dq.analysis.parameters.DBConnectionParameter;
import orgomg.cwm.foundation.softwaredeployment.DataProvider;

/**
 * DOC bzhou class global comment. Detailled comment
 */
public class DataProviderBuilder {

    static Logger log = Logger.getLogger(DataProviderBuilder.class);

    private boolean initialized = false;

    private DataProvider dataProvider;

    public boolean initializeDataProvider(DBConnectionParameter parameter) {
        if (initialized) {
            log.warn("Pattern already initialized. ");
            return false;
        }

        DBConnect connector = new DBConnect(parameter);
        try {
            dataProvider = TalendCwmFactory.createDataProvider(connector);
            String connectionName = parameter.getName();
            dataProvider.setName(connectionName);
            return true;
        } catch (SQLException e) {
            String mess = "Failed to create a data provider for the given connection parameters: " + e.getMessage();
            log.warn(mess, e);
        } finally {
            connector.closeConnection();
        }

        return false;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    /**
     * DOC bzhou Comment method "buildDriverForSQLExploer".
     * 
     * @param name
     * @param driverClassName
     * @param url
     * @param jars
     * @return
     */
    public ManagedDriver buildDriverForSQLExploer(String name, String driverClassName, String url, LinkedList<String> jars) {
        ManagedDriver driver = new ManagedDriver(SQLExplorerPlugin.getDefault().getDriverModel().createUniqueId());

        driver.setName(name);
        driver.setJars(jars);
        driver.setDriverClassName(driverClassName);
        driver.setUrl(url);
        SQLExplorerPlugin.getDefault().getDriverModel().addDriver(driver);

        return driver;
    }
}
