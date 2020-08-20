package ru.cloudstorage.server.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import ru.cloudstorage.clientserver.ByteCommand;
import ru.cloudstorage.clientserver.SubState;
import ru.cloudstorage.clientserver.WaitCallback;
import ru.cloudstorage.server.NetworkServer;

import java.nio.charset.StandardCharsets;

public class AuthUtil {
    private SubState subState;
    private int loginSize;
    private int passSize;
    private String login;
    private String password;

    public AuthUtil() {
        this.subState = SubState.IDLE;
    }

    public void authorise(ChannelHandlerContext ctx, ByteBuf buf, WaitCallback callback, WaitCallback authCallback) {
        getLogPass(buf, () -> {
            ByteBuf tmp = Unpooled.buffer();
            if (isAlreadyLogin()) {
                tmp.writeByte(ByteCommand.ALREADY_AUTH_COMMAND);
                ctx.writeAndFlush(tmp);
            } else if (isSuccessAut()) {
                tmp.writeByte(ByteCommand.SUCCESS_AUTH_COMMAND);
                ctx.writeAndFlush(tmp);
                authCallback.callback();
            } else {
                tmp.writeByte(ByteCommand.FAIL_AUTH_COMMAND);
                ctx.writeAndFlush(tmp);
            }
            callback.callback();
            tmp.clear();
        });
    }

    public void registration(ChannelHandlerContext ctx, ByteBuf buf, WaitCallback callback) {
        getLogPass(buf, () -> {
            ByteBuf tmp = Unpooled.buffer();
            if (isLoginExist()) {
                tmp.writeByte(ByteCommand.LOGIN_EXIST_COMMAND);
            } else {
                regToDB();
                tmp.writeByte(ByteCommand.SUCCESS_AUTH_COMMAND);
            }
            ctx.writeAndFlush(tmp);
            callback.callback();
        });
    }

    public void setSubState(SubState subState) {
        this.subState = subState;
    }

    public String getLogin() {
        return login;
    }

    private void getLogPass(ByteBuf buf, WaitCallback callback) {
        switch (subState) {
            case LOGIN_SIZE:
                if (setLoginSize(buf)) {
                    subState = SubState.LOGIN_STRING;
                }
                break;
            case LOGIN_STRING:
                if (setLogin(buf)) {
                    subState = SubState.PASS_SIZE;
                }
                break;
            case PASS_SIZE:
                if (setPassSize(buf)) {
                    subState = SubState.PASS_STRING;
                }
                break;
            case PASS_STRING:
                if (setPassword(buf)) {
                    callback.callback();
                    subState = SubState.IDLE;
                }
                break;
        }
    }

    private boolean setLoginSize(ByteBuf buf) {
        if (buf.readableBytes() >= 4) {
            loginSize = buf.readInt();
            return true;
        }
        return false;
    }

    private boolean setLogin(ByteBuf buf) {
        if (buf.readableBytes() >= loginSize) {
            byte[] loginBuf = new byte[loginSize];
            buf.readBytes(loginBuf);
            login = new String(loginBuf, StandardCharsets.UTF_8);
            return true;
        }
        return false;
    }

    private boolean setPassSize(ByteBuf buf) {
        if (buf.readableBytes() >= 4) {
            passSize = buf.readInt();
            return true;
        }
        return false;
    }

    private boolean setPassword(ByteBuf buf) {
        if (buf.readableBytes() >= passSize) {
            byte[] passBuf = new byte[passSize];
            buf.readBytes(passBuf);
            password = new String(passBuf, StandardCharsets.UTF_8);
            return true;
        }
        return false;
    }

    private boolean isAlreadyLogin() {
        return NetworkServer.getDatabaseService().isLogin(login);
    }

    private boolean isSuccessAut() {
        return NetworkServer.getDatabaseService().isAuthorise(login, password);
    }

    private boolean isLoginExist() {
        return NetworkServer.getDatabaseService().isLoginExist(login);
    }

    private void regToDB() {
        NetworkServer.getDatabaseService().registration(login, password);
    }
}
