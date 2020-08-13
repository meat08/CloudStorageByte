package ru.cloudstorage.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import ru.cloudstorage.clientserver.*;
import ru.cloudstorage.server.NetworkServer;
import ru.cloudstorage.server.util.AuthUtil;
import ru.cloudstorage.server.util.FileUtil;

import java.io.IOException;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(NetworkServer.class);
    private State currentState;
    private SubState subState;
    private String login;
    private AuthUtil auth;
    private FileUtil fileUtil;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        currentState = State.IDLE;
        subState = SubState.IDLE;
        System.out.println("Клиент подключился. Addr: " + ctx.channel().remoteAddress());
        logger.info("Клиент подключился. Addr: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Клиент отключился. Addr: " + ctx.channel().remoteAddress() + " Login: " + login);
        NetworkServer.getDatabaseService().setIsLogin(login, false);
        logger.info("Клиент отключился. Addr: " + ctx.channel().remoteAddress() + " Login: " + login);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf buf = (ByteBuf) msg;
        while (buf.readableBytes() > 0) {
            switch (currentState) {
                case IDLE: {
                    byte command = buf.readByte();
                    if (command == ByteCommand.AUTHORISE_COMMAND) {
                        auth = new AuthUtil();
                        currentState = State.AUTH;
                        subState = SubState.LOGIN_SIZE;
                    } else if (command == ByteCommand.REGISTRATION_COMMAND) {
                        auth = new AuthUtil();
                        currentState = State.REGISTRATION;
                        subState = SubState.LOGIN_SIZE;
                    } else if (command == ByteCommand.GET_ROOT_PATH_COMMAND) {
                        fileUtil.sendClientDir(ctx, login);
                        currentState = State.IDLE;
                    } else if (command == ByteCommand.LIST_COMMAND) {
                        currentState = State.FILE_LIST;
                        subState = SubState.PATH_SIZE;
                    } else if (command == ByteCommand.TO_SERVER_COMMAND) {
                        currentState = State.FILE_GET;
                        subState = SubState.PATH_SIZE;
                    } else if (command == ByteCommand.DELETE_FILE_COMMAND) {
                        currentState = State.FILE_DELETE;
                        subState = SubState.PATH_SIZE;
                    } else if (command == ByteCommand.FROM_SERVER_COMMAND) {
                        currentState = State.FILE_PUT;
                        subState = SubState.PATH_SIZE;
                    }
                    break;
                }
                case AUTH: {
                    receiveLoginPassword(buf, () -> {
                        ByteBuf tmp = Unpooled.buffer();
                        if (auth.isAlreadyLogin()) {
                            tmp.writeByte(ByteCommand.ALREADY_AUTH_COMMAND);
                            ctx.writeAndFlush(tmp);
                        } else if (auth.isSuccessAut()) {
                            tmp.writeByte(ByteCommand.SUCCESS_AUTH_COMMAND);
                            this.login = auth.getLogin();
                            fileUtil = new FileUtil();
                            ctx.writeAndFlush(tmp);
                        } else {
                            tmp.writeByte(ByteCommand.FAIL_AUTH_COMMAND);
                            ctx.writeAndFlush(tmp);
                        }
                        tmp.clear();
                    });

                    break;
                }
                case REGISTRATION: {
                    receiveLoginPassword(buf, () -> {
                        ByteBuf tmp = Unpooled.buffer();
                        if (auth.isLoginExist()) {
                            tmp.writeByte(ByteCommand.LOGIN_EXIST_COMMAND);
                        } else {
                            auth.registration();
                            tmp.writeByte(ByteCommand.SUCCESS_AUTH_COMMAND);
                        }
                        ctx.writeAndFlush(tmp);
                    });
                    break;
                }
                case FILE_LIST: {
                    switch (subState) {
                        case PATH_SIZE:
                            if (fileUtil.getPathSize(buf)) {
                                subState = SubState.PATH_STRING;
                            }
                            break;
                        case PATH_STRING:
                            if (fileUtil.getPathName(buf, false)) {
                                fileUtil.sendFileList(ctx);
                                subState = SubState.IDLE;
                                currentState = State.IDLE;
                            }
                            break;
                    }
                    break;
                }
                case FILE_GET: {
                    switch (subState) {
                        case PATH_SIZE:
                            if (fileUtil.getPathSize(buf)) {
                                subState = SubState.PATH_STRING;
                            }
                            break;
                        case PATH_STRING:
                            if (fileUtil.getPathName(buf, true)) {
                                subState = SubState.FILE_SIZE;
                            }
                            break;
                        case FILE_SIZE:
                            if (fileUtil.getFileSize(buf)) {
                                subState = SubState.FILE;
                            }
                            break;
                        case FILE:
                            fileUtil.getFile(ctx, buf, () -> {
                                subState = SubState.IDLE;
                                currentState = State.IDLE;
                            });
                            break;
                    }
                    break;
                }
                case FILE_PUT: {
                    switch (subState) {
                        case PATH_SIZE:
                            if (fileUtil.getPathSize(buf)) {
                                subState = SubState.PATH_STRING;
                            }
                            break;
                        case PATH_STRING:
                            if (fileUtil.getPathName(buf, true)) {
                                fileUtil.putFile(ctx, () -> {
                                    subState = SubState.IDLE;
                                    currentState = State.IDLE;
                                });
                            }
                            break;
                    }
                    break;
                }
                case FILE_DELETE: {
                    switch (subState) {
                        case PATH_SIZE:
                            if (fileUtil.getPathSize(buf)) {
                                subState = SubState.PATH_STRING;
                            }
                            break;
                        case PATH_STRING:
                            if (fileUtil.getPathName(buf, true)) {
                                fileUtil.deleteFile();
                                subState = SubState.IDLE;
                                currentState = State.IDLE;
                            }
                            break;
                    }
                    break;
                }
            }
        }
        buf.release();
    }

    private void receiveLoginPassword(ByteBuf buf, WaitCallback callback) {
        switch (subState) {
            case LOGIN_SIZE:
                if (auth.setLoginSize(buf)) {
                    subState = SubState.LOGIN_STRING;
                }
                break;
            case LOGIN_STRING:
                if (auth.setLogin(buf)) {
                    subState = SubState.PASS_SIZE;
                }
                break;
            case PASS_SIZE:
                if (auth.setPassSize(buf)) {
                    subState = SubState.PASS_STRING;
                }
                break;
            case PASS_STRING:
                if (auth.setPassword(buf)) {
                    callback.callback();
                    currentState = State.IDLE;
                    subState = SubState.IDLE;
                }
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            System.out.println("Клиент разорвал соединение");
            logger.info("Клиент разорвал соединение. Adr: " + ctx.channel().remoteAddress() + " Login: " + login);
            NetworkServer.getDatabaseService().setIsLogin(login, false);
        } else {
            cause.printStackTrace();
            logger.fatal("Возникло исключение: " + cause.getMessage());
        }
        ctx.close();
    }
}
