package ru.cloudstorage.client.network;

import ru.cloudstorage.client.util.ClientProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

public class Network {
    private static final Network ourInstance = new Network();
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8180;
    private DataOutputStream out;
    private DataInputStream in;
    private SocketChannel currentChannel;

    public static Network getInstance() {
        return ourInstance;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public DataInputStream getIn() {
        return in;
    }

    public SocketChannel getCurrentChannel() {
        return currentChannel;
    }

    private Network() {
    }

    public void start(CountDownLatch countDownLatch) {
        try {
            ClientProperties properties = new ClientProperties();
            String host = properties.getHost();
            int port = properties.getPort();
            if (host == null) {
                System.out.println("Файл конфигурации недоступен. Использован хост по умолчанию.");
                host = DEFAULT_HOST;
            }
            if (port == 0) {
                System.out.println("Файл конфигурации недоступен. Использован порт по умолчанию.");
                port = DEFAULT_PORT;
            }
            InetSocketAddress serverAddress = new InetSocketAddress(host, port);
            currentChannel = SocketChannel.open(serverAddress);
            out = new DataOutputStream(currentChannel.socket().getOutputStream());
            in = new DataInputStream(currentChannel.socket().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        countDownLatch.countDown();

    }

    public void stop() {
        try {
            in.close();
            out.close();
            currentChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
