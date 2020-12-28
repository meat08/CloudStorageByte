package ru.cloudstorage.client.util;

import javafx.application.Platform;
import javafx.scene.control.Label;
import ru.cloudstorage.client.controllers.RightPanelController;
import ru.cloudstorage.client.network.Network;
import ru.cloudstorage.clientserver.ByteCommand;
import ru.cloudstorage.clientserver.WaitCallback;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AuthUtilClient {

    public static void authorise(String login, String password, Label loginLabel, WaitCallback callback) {
        new Thread(() -> {
            try {
                sendLoginPassword(login, password, false);
                byte result = Network.getInstance().getIn().readByte();
                if (result == ByteCommand.ALREADY_AUTH_COMMAND) {
                    setLabelText(loginLabel, "Клиент с таким логином уже подключен");
                } else if (result == ByteCommand.SUCCESS_AUTH_COMMAND) {
                    setLabelText(loginLabel, "Успешная авторизация");
                    Platform.runLater(callback::callback);
                } else  if (result == ByteCommand.FAIL_AUTH_COMMAND){
                    setLabelText(loginLabel, "Неверный логин или пароль");
                } else {
                    setLabelText(loginLabel, "Неизвестная команда");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void registration(String login, String password, Label loginLabel) {
        new Thread(() -> {
            try {
                sendLoginPassword(login, password, true);
                byte result = Network.getInstance().getIn().readByte();
                if (result == ByteCommand.LOGIN_EXIST_COMMAND) {
                    setLabelText(loginLabel, "Клиент с таким логином уже зарегистрирован!");
                } else if (result == ByteCommand.SUCCESS_AUTH_COMMAND) {
                    setLabelText(loginLabel, "Успешная регистрация. Теперь вы можете войти под своим логином и паролем.");
                } else {
                    setLabelText(loginLabel, "Неизвестная команда");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void sendLoginPassword(String login, String password, boolean isRegistration) throws IOException {
        int bufSize = 1 + 4 + login.length() + 4 + password.length();
        ByteBuffer buf = ByteBuffer.allocate(bufSize);
        if (isRegistration) {
            buf.put(ByteCommand.REGISTRATION_COMMAND);
        } else {
            buf.put(ByteCommand.AUTHORISE_COMMAND);
        }
        buf.putInt(login.length());
        buf.put(login.getBytes());
        buf.putInt(password.length());
        buf.put(password.getBytes());
        buf.flip();
        Network.getInstance().getCurrentChannel().write(buf);
    }

    public static void setHomeDir(RightPanelController rightPanelController, WaitCallback callback) {
        new Thread(() -> {
            try {
                Network.getInstance().getOut().write(ByteCommand.GET_ROOT_PATH_COMMAND);
                DataInputStream in = Network.getInstance().getIn();
                byte result = in.readByte();
                if (result == ByteCommand.GET_ROOT_PATH_COMMAND) {
                    int rootPathSize = in.readInt();
                    byte[] rootPathBuf = new byte[rootPathSize];
                    int readByte = 0;
                    while (readByte < rootPathSize) {
                        readByte += in.read(rootPathBuf);
                    }

                    int clientPathSize = in.readInt();
                    byte[] clientPathBuf = new byte[clientPathSize];
                    readByte = 0;
                    while (readByte < clientPathSize) {
                        readByte += in.read(clientPathBuf);
                    }

                    String rootPath = new String(rootPathBuf, StandardCharsets.UTF_8);
                    String clientPath = new String(clientPathBuf, StandardCharsets.UTF_8);

                    rightPanelController.setServerPaths(rootPath, clientPath);
                    Platform.runLater(callback::callback);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private static void setLabelText(Label label, String text) {
        Platform.runLater(() -> label.setText(text));
    }
}
