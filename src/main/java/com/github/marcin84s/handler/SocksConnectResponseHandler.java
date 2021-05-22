package com.github.marcin84s.handler;

import com.github.marcin84s.consts.SocksConst;
import com.github.marcin84s.handler.msgs.*;
import com.github.marcin84s.netty.Netty;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocksConnectResponseHandler extends ChannelOutboundHandlerAdapter {
    public static final Logger log = LoggerFactory.getLogger(SocksConnectResponseHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SocksIP4ConnectRequest) {
            SocksIP4ConnectRequest connectRequest = (SocksIP4ConnectRequest) msg;

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

                    if (connectRequest.getVersion() == SocksConst.VER4) {
                        sendResponse(ctx, connectRequest, SocksConst.REPLY_COMMAND_GRANTED);
                    } else {
                        sendResponseSocks5(ctx, connectRequest, SocksConst.STATUS_GRANTED);
                    }
                } else {
                    log.error("connect failure {}", connectRequest.getEndpoint());
                    log.error("", f.cause());

                    if (connectRequest.getVersion() == SocksConst.VER4) {
                        sendResponse(ctx, connectRequest, SocksConst.REPLY_COMMAND_REJECTED);
                    } else {
                        sendResponseSocks5(ctx, connectRequest, SocksConst.STATUS_GENERAL_FAILURE);
                    }
                }
            });

            ctx.pipeline().remove(this);
        } else if (msg instanceof Socks4ConnectReject) {
            sendResponse(ctx, null, SocksConst.REPLY_COMMAND_REJECTED);
            ctx.close();
        } else if (msg instanceof Socks5AuthNotSupported) {
            sendResponseSocks5(ctx, SocksConst.AUTH_NOT_SUPPORTED);
        } else if (msg instanceof Socks5AcceptedAuth) {
            Socks5AcceptedAuth socks5AcceptAuth = (Socks5AcceptedAuth) msg;
            sendResponseSocks5(ctx, socks5AcceptAuth.getAuth());
        } else if (msg instanceof SocksCloseConnection) {
            ctx.close();
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void sendResponseSocks5(ChannelHandlerContext ctx, SocksIP4ConnectRequest connectRequest, byte status) {
        ByteBuf out = ctx.alloc().ioBuffer();
        out.writeByte(SocksConst.VER5);
        out.writeByte(status);
        out.writeByte(0);
        out.writeByte(SocksConst.IPV4);
        out.writeInt(connectRequest.getDestIp());
        out.writeShort(connectRequest.getPort());
        ctx.writeAndFlush(out);
    }

    private void sendResponseSocks5(ChannelHandlerContext ctx, byte auth) {
        ByteBuf out = ctx.alloc().ioBuffer();
        out.writeByte(SocksConst.VER5);
        out.writeByte(auth);
        ctx.writeAndFlush(out);
    }

    private void sendResponse(ChannelHandlerContext ctx, byte command) {
        ByteBuf out = ctx.alloc().ioBuffer();
        out.writeByte(0);
        out.writeByte(command);
        ctx.writeAndFlush(out);
    }

    private void sendResponse(ChannelHandlerContext ctx, SocksIP4ConnectRequest connectRequest, byte command) {
        ByteBuf out = ctx.alloc().ioBuffer();
        out.writeByte(0);
        out.writeByte(command);
        if(connectRequest != null) {
            out.writeShort(connectRequest.getPort());
            out.writeInt(connectRequest.getDestIp());
        } else {
            out.writeShort(0);
            out.writeInt(0);
        }
        ctx.writeAndFlush(out);
    }
}
