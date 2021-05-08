package com.github.marcin84s.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class IncomingTrafficHandler extends ChannelInboundHandlerAdapter {
    private final Channel remote;

    public IncomingTrafficHandler(Channel remote) {
        this.remote = remote;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        remote.writeAndFlush(msg);
    }


}
