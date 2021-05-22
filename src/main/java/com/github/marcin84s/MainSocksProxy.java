package com.github.marcin84s;

import com.github.marcin84s.netty.Netty;
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
            log.info("starting server port {}", port);
            Netty.bindSocks4Proxy(port).sync().channel().closeFuture().sync();
        } finally {
            Netty.shutdownWorker();
        }
    }
}
