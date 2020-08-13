package ru.cloudstorage.server;

import org.apache.log4j.Logger;
import ru.cloudstorage.server.util.ServerProperties;

public class CloudServer {
    private static final int DEFAULT_PORT = 8180;
    private static final Logger logger = Logger.getLogger(NetworkServer.class);

    private static int getServerPort() {
        try {
            return new ServerProperties().getPort();
        } catch (Exception e) {
            System.out.println("Файл конфигурации недоступен. Использован порт по умолчанию.");
            logger.info("Файл конфигурации недоступен. Использован порт по умолчанию.");
            return DEFAULT_PORT;
        }
    }


    public static void main(String[] args) throws Exception {
        int port = getServerPort();
        new NetworkServer(port).run();
    }
}
