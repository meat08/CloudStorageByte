package ru.cloudstorage.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientProperties {
    private final Properties properties;

    public ClientProperties() throws IOException {
        InputStream fis = getClass().getResourceAsStream("/client.properties");
        this.properties = new Properties();
        properties.load(fis);
    }

    public String getHost() {
        return properties.getProperty("cl.host");
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("cl.port"));
    }
}
