package ru.cloudstorage.server.util;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import ru.cloudstorage.clientserver.*;
import ru.cloudstorage.server.NetworkServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {
    private static final Path DEFAULT_ROOT_DIR = Paths.get(".", "data");
    private static final Logger logger = Logger.getLogger(NetworkServer.class);
    private static final int BUFFER_SIZE = 1024 * 1024 * 10;

    private SubState subState;
    private int pathSize;
    private String pathName;
    private long fileSize;
    private long receivedFileSize;

    public FileUtil() {
        this.subState = SubState.IDLE;
    }

    public void fileList(ChannelHandlerContext ctx, ByteBuf buf, WaitCallback callback) throws IOException {
        switch (subState) {
            case PATH_SIZE:
                if (getPathSize(buf)) {
                    subState = SubState.PATH_STRING;
                }
                break;
            case PATH_STRING:
                if (getPathName(buf, false)) {
                    sendFileList(ctx);
                    subState = SubState.IDLE;
                    callback.callback();
                }
                break;
        }
    }

    public void transferFile(ChannelHandlerContext ctx, ByteBuf buf, boolean isGet, WaitCallback callback) {
        switch (subState) {
            case PATH_SIZE:
                if (getPathSize(buf)) {
                    subState = SubState.PATH_STRING;
                }
                break;
            case PATH_STRING:
                if (getPathName(buf, true)) {
                    if (isGet) {
                        subState = SubState.FILE_SIZE;
                    } else {
                        putFile(ctx,callback);
                    }
                }
                break;
            case FILE_SIZE:
                if (getFileSize(buf)) {
                    subState = SubState.FILE;
                }
                break;
            case FILE:
                if (isGet) {
                    getFile(ctx, buf, callback);
                }
                break;
        }
    }

    public void deleteFile(ByteBuf buf, WaitCallback callback) {
        switch (subState) {
            case PATH_SIZE:
                if (getPathSize(buf)) {
                    subState = SubState.PATH_STRING;
                }
                break;
            case PATH_STRING:
                if (getPathName(buf, true)) {
                    try {
                        Files.delete(Paths.get(pathName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    subState = SubState.IDLE;
                    callback.callback();
                }
                break;
        }
    }

    public void sendClientDir(ChannelHandlerContext ctx, String login) throws IOException {
        String[] paths = createHomeDir(login);
        ByteBuf tmp = Unpooled.buffer();
        tmp.writeByte(ByteCommand.GET_ROOT_PATH_COMMAND);
        tmp.writeInt(paths[0].length());
        tmp.writeBytes(paths[0].getBytes());
        tmp.writeInt(paths[1].length());
        tmp.writeBytes(paths[1].getBytes());
        ctx.writeAndFlush(tmp);
        tmp.clear();
    }

    public void setSubState(SubState subState) {
        this.subState = subState;
    }

    private String[] createHomeDir(String login) throws IOException {
        Path clientPath;
        String[] paths = new String[2];
        try {
            String rootDir = new ServerProperties().getRootDir();
            clientPath = Paths.get(rootDir, login);
            paths[0] = rootDir;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Файл конфигурации не обнаружен. Использован каталог по умолчанию.");
            logger.info("Файл конфигурации не обнаружен. Использован каталог по умолчанию.");
            clientPath = Paths.get(DEFAULT_ROOT_DIR.toString(), login);
            paths[0] = DEFAULT_ROOT_DIR.toString();
        }
        if (!Files.exists(clientPath)) {
            Files.createDirectories(clientPath);
        }
        paths[1] = clientPath.toString();
        return paths;
    }

    private String getFileListJson() throws IOException {
        Gson gson = new Gson();
        List<FileInfo> list = Files.list(Paths.get(pathName)).map(FileInfo::new).collect(Collectors.toList());
        return gson.toJson(list);
    }

    private void sendFileList(ChannelHandlerContext ctx) throws IOException {
        String fileList = getFileListJson();
        ByteBuf tmp = Unpooled.buffer();
        tmp.writeByte(ByteCommand.LIST_COMMAND);
        tmp.writeInt(fileList.length());
        tmp.writeBytes(fileList.getBytes());
        ctx.writeAndFlush(tmp);
        tmp.clear();
    }

    private boolean getPathSize(ByteBuf buf) {
        if (buf.readableBytes() >= Integer.BYTES) {
            pathSize = buf.readInt();
            return true;
        }
        return false;
    }

    private boolean getPathName(ByteBuf buf, boolean isFile) {
        if (buf.readableBytes() >= pathSize) {
            byte[] passBuf = new byte[pathSize];
            buf.readBytes(passBuf);
            pathName = new String(passBuf, StandardCharsets.UTF_8);
            if (isFile) {
                receivedFileSize = -1L;
            }
            return true;
        }
        return false;
    }

    private boolean getFileSize(ByteBuf buf) {
        if (buf.readableBytes() >= Long.BYTES) {
            fileSize = buf.readLong();
            receivedFileSize = -1;
            return true;
        }
        return false;
    }

    private void getFile(ChannelHandlerContext ctx, ByteBuf buf, WaitCallback callback) {
        boolean append = true;
        if (receivedFileSize == -1) {
            append = false;
            receivedFileSize = 0;
        }
        try (FileOutputStream out = new FileOutputStream(pathName, append)) {
            if (fileSize == 0) {
                sendFinish(ctx);
                callback.callback();
                subState = SubState.IDLE;
            }
            while (buf.readableBytes() > 0) {
                int write = out.getChannel().write(buf.nioBuffer());
                receivedFileSize += write;
                buf.readerIndex(buf.readerIndex() + write);
                if (receivedFileSize == fileSize) {
                    sendFinish(ctx);
                    callback.callback();
                    subState = SubState.IDLE;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void putFile(ChannelHandlerContext ctx, WaitCallback callback) {
        new Thread(() -> {
            try {
                File srcFile = Paths.get(pathName).toFile();
                FileInputStream in = new FileInputStream(srcFile);
                long fileSize = srcFile.length();
                ByteBuf tmp = Unpooled.buffer();
                tmp.writeLong(fileSize);
                ctx.writeAndFlush(tmp);

                long bytesSend = 0;
                while (bytesSend < fileSize) {
                    tmp = Unpooled.buffer();
                    int readByte = tmp.writeBytes(in.getChannel(), bytesSend, BUFFER_SIZE);
                    bytesSend += readByte;
                    ctx.writeAndFlush(tmp);
                }
                in.close();
                callback.callback();
                subState = SubState.IDLE;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendFinish(ChannelHandlerContext ctx) {
        ByteBuf tmp = Unpooled.buffer();
        tmp.writeByte(ByteCommand.FINISH_COMMAND);
        ctx.writeAndFlush(tmp);
        tmp.clear();
    }
}
