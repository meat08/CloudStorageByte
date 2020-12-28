package ru.cloudstorage.server.database;

import org.apache.log4j.Logger;
import ru.cloudstorage.server.util.ServerProperties;

import java.sql.*;

public class DatabaseService {
    private Connection connection = null;
    private static final String URL = "jdbc:sqlite:" + new ServerProperties().getSqlDir();
    private static final Logger logger = Logger.getLogger(DatabaseService.class);

    public void start() {
        try {
            connection = DriverManager.getConnection(URL);
            resetIsLogin();
            logger.info("База данных подключена");
            System.out.println("База данных подключена");
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных");
            logger.fatal("Ошибка подключения к базе данных");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void stop() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAuthorise(String login, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ? AND password = ?"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setIsLogin(String login, boolean isLogin) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = ? WHERE login = ?"
            );
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, login);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isLogin(String login) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ? AND isLogin = 1"
            );
            statement.setString(1, login);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isLoginExist(String login) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ?"
            );
            statement.setString(1, login);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void registration(String login, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (login , password) VALUES (? , ?)"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void resetIsLogin() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET isLogin = 0"
            );
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
