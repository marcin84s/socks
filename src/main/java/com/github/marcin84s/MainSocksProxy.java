package com.github.marcin84s;

import com.github.marcin84s.handler.Socks4ConnectRequestDecoder;
import com.github.marcin84s.handler.Socks4ConnectResponseHandler;
import com.github.marcin84s.netty.Netty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainSocksProxy {
    public static final Logger log = LoggerFactory.getLogger(MainSocksProxy.class);

    public static void main(String[] args) throws InterruptedException {
        int port = 4321;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try {
            log.debug("starting server port {}", port);
            Netty.createSocks4Proxy(port).sync().channel().closeFuture().sync();
        } finally {
            Netty.shutdownWorker();
        }
    }
}
