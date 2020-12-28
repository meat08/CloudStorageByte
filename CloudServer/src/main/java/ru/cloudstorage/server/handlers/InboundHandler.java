package ru.cloudstorage.server.handlers;

import io.netty.buffer.ByteBuf;
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
    private String login;
    private AuthUtil auth;
    private FileUtil fileUtil;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        currentState = State.IDLE;
        auth = new AuthUtil();
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
                        currentState = State.AUTH;
                        auth.setSubState(SubState.LOGIN_SIZE);
                    } else if (command == ByteCommand.REGISTRATION_COMMAND) {
                        currentState = State.REGISTRATION;
                        auth.setSubState(SubState.LOGIN_SIZE);
                    } else if (command == ByteCommand.GET_ROOT_PATH_COMMAND) {
                        fileUtil.sendClientDir(ctx, login);
                        currentState = State.IDLE;
                    } else if (command == ByteCommand.LIST_COMMAND) {
                        currentState = State.FILE_LIST;
                        fileUtil.setSubState(SubState.PATH_SIZE);
                    } else if (command == ByteCommand.TO_SERVER_COMMAND) {
                        currentState = State.FILE_GET;
                        fileUtil.setSubState(SubState.PATH_SIZE);
                    } else if (command == ByteCommand.DELETE_FILE_COMMAND) {
                        currentState = State.FILE_DELETE;
                        fileUtil.setSubState(SubState.PATH_SIZE);
                    } else if (command == ByteCommand.FROM_SERVER_COMMAND) {
                        currentState = State.FILE_PUT;
                        fileUtil.setSubState(SubState.PATH_SIZE);
                    }
                    break;
                }
                case AUTH: {
                    auth.authorise(ctx, buf, () -> currentState = State.IDLE, () -> {
                        fileUtil = new FileUtil();
                        login = auth.getLogin();
                    });
                    break;
                }
                case REGISTRATION: {
                    auth.registration(ctx, buf, () -> currentState = State.IDLE);
                    break;
                }
                case FILE_LIST: {
                    fileUtil.fileList(ctx, buf, () -> currentState = State.IDLE);
                    break;
                }
                case FILE_GET: {
                    fileUtil.transferFile(ctx, buf, true, () -> currentState = State.IDLE);
                    break;
                }
                case FILE_PUT: {
                    fileUtil.transferFile(ctx, buf, false, () -> currentState = State.IDLE);
                    break;
                }
                case FILE_DELETE: {
                    fileUtil.deleteFile(buf, () -> currentState = State.IDLE);
                    break;
                }
            }
        }
        buf.release();
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
