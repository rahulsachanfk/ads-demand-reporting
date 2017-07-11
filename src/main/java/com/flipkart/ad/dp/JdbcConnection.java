package com.flipkart.ad.dp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by rahul.sachan on 10/03/17.
 */
public class JdbcConnection {
    private  static Connection con = null;
    private JdbcConnection(){}
    public static Connection getJdbcConnection() {
        if (con != null){
            return con;
        }
        try {
            ConfigProvider cf = ConfigProvider.getInstance();
            String url = ConfigProvider.getInstance().get(Constant.DB_CONFIGURATION_URL, "");
            String username = ConfigProvider.getInstance().get(Constant.DB_CONFIGURATION_USER, "");
            String password = ConfigProvider.getInstance().get(Constant.DB_CONFIGURATION_PASSWORD, "");
            System.out.println(url);
            con = DriverManager.getConnection(url, username, password);
            //con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/neo", "root", "");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return con;
    }

}
