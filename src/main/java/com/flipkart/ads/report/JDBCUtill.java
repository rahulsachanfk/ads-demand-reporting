package com.flipkart.ads.report;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by rahul.sachan on 13/07/17.
 */

public class JDBCUtill {
    private static Connection con = null;

    private JDBCUtill() {
    }

    public static Connection getJdbcConnection(String url,String username,String password) throws SQLException {

            return  DriverManager.getConnection(url, username, password);
    }

}
