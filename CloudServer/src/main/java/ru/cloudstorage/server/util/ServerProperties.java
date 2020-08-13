package ru.cloudstorage.server.util;

import java.io.InputStream;
import java.util.Properties;

public class ServerProperties {

    private final Properties properties;

    public ServerProperties() throws Exception{
        InputStream fis = getClass().getResourceAsStream("/server.properties");
        this.properties = new Properties();
        properties.load(fis);
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("srv.port"));
    }

    public String getRootDir() {
        return properties.getProperty("srv.rootDir");
    }
}
