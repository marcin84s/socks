package com.github.marcin84s.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netty {
    public static final Logger log = LoggerFactory.getLogger(Netty.class);
    public static EventLoopGroup workerGroup = new NioEventLoopGroup();
    private static Bootstrap bootstrap = null;
    private static ServerBootstrap serverBootstrap = null;

    public static synchronized ServerBootstrap getServerBootstrap(ChannelInitializer<SocketChannel> channelInitializer) {
        if (serverBootstrap == null) {
            serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelInitializer)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
        }
        return serverBootstrap;
    }

    public static synchronized Bootstrap getBootstrap() {
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler());
                }
            });
        }

        return bootstrap;
    }

    public static synchronized void shutdownWorker() {
            workerGroup.shutdownGracefully();
    }
}
