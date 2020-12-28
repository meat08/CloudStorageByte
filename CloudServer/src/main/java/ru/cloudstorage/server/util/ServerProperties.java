package ru.cloudstorage.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerProperties {

    private final Properties properties;

    public ServerProperties() {
        InputStream fis = getClass().getResourceAsStream("/config/server.properties");
        this.properties = new Properties();
        try {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("srv.port"));
    }

    public String getRootDir() {
        return properties.getProperty("srv.rootDir");
    }

    public String getSqlDir() {
        return properties.getProperty("srv.sqlDir");
    }
}
