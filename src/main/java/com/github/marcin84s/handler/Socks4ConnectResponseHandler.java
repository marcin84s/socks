package com.github.marcin84s.handler;

import com.github.marcin84s.netty.Netty;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks4ConnectResponseHandler extends ChannelOutboundHandlerAdapter {
    public static final Logger log = LoggerFactory.getLogger(Socks4ConnectResponseHandler.class);

    private static final byte REPLY_COMMAND_GRANTED = (byte) 0x5A;
    private static final byte REPLY_COMMAND_REJECTED = (byte) 0x5B;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Socks4ConnectRequest connectRequest = (Socks4ConnectRequest) msg;

        ChannelFuture connectChannelFuture = Netty.getBootstrap().connect(connectRequest.getDestAddress(), connectRequest.getPort());
        connectChannelFuture.addListener(f -> {
            if (f.isSuccess()) {
                Channel connectChannel = connectChannelFuture.channel();
                log.debug("connected {}", connectChannel);

                ctx.pipeline().addLast(new IncomingTrafficHandler(connectChannel));
                connectChannel.pipeline().addLast(new IncomingTrafficHandler(ctx.channel()));

                connectChannel.closeFuture().addListener(c -> {
                    log.debug("closing {}", connectChannel);
                    ctx.channel().close();
                });

                ctx.channel().closeFuture().addListener(c -> {
                    log.debug("closing {}", ctx.channel());
                    connectChannel.close();
                });

                sendResponse(ctx, connectRequest, REPLY_COMMAND_GRANTED);
            } else {
                log.error("connect failure {}", connectRequest.getEndpoint());
                f.cause().printStackTrace();

                sendResponse(ctx, connectRequest, REPLY_COMMAND_REJECTED);
            }
        });

        ctx.pipeline().remove(this);
    }

    private void sendResponse(ChannelHandlerContext ctx, Socks4ConnectRequest connectRequest, byte command) {
        ByteBuf out = ctx.alloc().ioBuffer();
        out.writeByte(0);
        out.writeByte(command);
        out.writeShort(connectRequest.getPort());
        out.writeInt(connectRequest.getDestIp());
        ctx.writeAndFlush(out);
    }
}
