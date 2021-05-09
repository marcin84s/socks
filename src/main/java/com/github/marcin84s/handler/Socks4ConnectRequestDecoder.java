package com.github.marcin84s.handler;

import com.github.marcin84s.handler.msgs.Socks4ConnectNotSupportedVersion5;
import com.github.marcin84s.handler.msgs.Socks4ConnectReject;
import com.github.marcin84s.handler.msgs.Socks4ConnectRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.commons.lang3.Conversion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;

enum ConnectInputState {
    VERSION,
    COMMAND_CODE,
    DEST_PORT,
    DEST_IP,
    USERID_AND_NULL,
}

public class Socks4ConnectRequestDecoder extends ReplayingDecoder<ConnectInputState> {
    public static final Logger log = LoggerFactory.getLogger(Socks4ConnectRequestDecoder.class);

    private static final int COMMAND_CONNECT = 1;

    private Socks4ConnectRequest socks4Connect;
    private ByteArrayOutputStream baos;

    public Socks4ConnectRequestDecoder() {
        super(ConnectInputState.VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case VERSION:
                byte version = in.readByte();
                if (version != 4) {
                    log.info("version != 4 -> {}", version);
                    if (version == 5) {
                        in.readBytes(new byte[3]);
                        ctx.writeAndFlush(new Socks4ConnectNotSupportedVersion5());
                        return;
                    }

                    ctx.pipeline().remove(this);
                    ctx.writeAndFlush(new Socks4ConnectReject());
                    return;
                }

                checkpoint(ConnectInputState.COMMAND_CODE);

            case COMMAND_CODE:
                byte command = in.readByte();
                checkpoint(ConnectInputState.DEST_PORT);

                if (command != COMMAND_CONNECT) {
                    log.info("command != 1 (CONNECT) -> {}", command);
                    ctx.pipeline().remove(this);
                    ctx.writeAndFlush(new Socks4ConnectReject());
                    return;
                }

            case DEST_PORT:
                int port = in.readShort();
                checkpoint(ConnectInputState.DEST_IP);
                socks4Connect = new Socks4ConnectRequest();
                socks4Connect.setPort(port);

            case DEST_IP:
                int ip = in.readIntLE();
                checkpoint(ConnectInputState.USERID_AND_NULL);
                byte ipArray[] = new byte[4];
                Conversion.intToByteArray(ip, 0, ipArray, 0, 4);
                InetAddress inetAddress = InetAddress.getByAddress(ipArray);
                socks4Connect.setDestIp(ip);
                socks4Connect.setDestAddress(inetAddress);
                baos = new ByteArrayOutputStream();

            case USERID_AND_NULL:
                byte b = 0;
                while ((b = in.readByte()) != 0) {
                    baos.write(b);
                }

                String userId = new String(baos.toByteArray());
                socks4Connect.setUserId(userId);
                baos = null;

                ctx.pipeline().remove(this);
                ctx.writeAndFlush(socks4Connect);
        }
    }
}

