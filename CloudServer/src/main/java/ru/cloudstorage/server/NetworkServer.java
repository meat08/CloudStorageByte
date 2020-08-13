package ru.cloudstorage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;
import ru.cloudstorage.server.database.DatabaseService;
import ru.cloudstorage.server.handlers.InboundHandler;

public class NetworkServer {
    private final int port;
    private static final DatabaseService databaseService = new DatabaseService();
    private static final Logger logger = Logger.getLogger(NetworkServer.class);

    public NetworkServer(int port) {
        this.port = port;
    }

    public static DatabaseService getDatabaseService() {
        return databaseService;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(
                                    new InboundHandler()
                            );
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            logger.info("Сервер запущен на порту " + port);
            System.out.println("Сервер запущен на порту " + port);
            databaseService.start();
            f.channel().closeFuture().sync();
        } finally {
            logger.info("Сервер остановлен.");
            System.out.println("Сервер остановлен.");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            databaseService.stop();
        }
    }

}
