package com.github.marcin84s.handler;

import com.github.marcin84s.consts.SocksConst;
import com.github.marcin84s.handler.msgs.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.commons.lang3.Conversion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

enum ConnectInputState {
    VERSION,
    COMMAND_CODE,
    DEST_PORT,
    DEST_IP,
    USERID_AND_NULL,
    VERSION5_NAUTH,
    VERSION5_CLIENT_AUTH
}

public class SocksConnectRequestDecoder extends ReplayingDecoder<ConnectInputState> {
    public static final Logger log = LoggerFactory.getLogger(SocksConnectRequestDecoder.class);

    private SocksIP4ConnectRequest socksIP4Connect;
    private ByteArrayOutputStream baos;

    public SocksConnectRequestDecoder() {
        super(ConnectInputState.VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.trace("Socks4ConnectRequestDecoder state {}", state());

        switch (state()) {
            case VERSION:
                byte version = in.readByte();
                if (version == 4) {
                    checkpoint(ConnectInputState.COMMAND_CODE);
                } else if (version == 5) {
                    checkpoint(ConnectInputState.VERSION5_NAUTH);
                    return;
                } else {
                    ctx.pipeline().remove(this);
                    ctx.writeAndFlush(new Socks4ConnectReject());
                    return;
                }

            case COMMAND_CODE:
                byte command = in.readByte();
                checkpoint(ConnectInputState.DEST_PORT);

                if (command != SocksConst.COMMAND_CONNECT) {
                    log.info("command != {} (CONNECT) -> {}", SocksConst.COMMAND_CONNECT, command);
                    ctx.pipeline().remove(this);
                    ctx.writeAndFlush(new Socks4ConnectReject());
                    return;
                }

            case DEST_PORT:
                int port = in.readShort();
                checkpoint(ConnectInputState.DEST_IP);
                socksIP4Connect = new SocksIP4ConnectRequest(SocksConst.VER4);
                socksIP4Connect.setPort(port);

            case DEST_IP:
                int ip = in.readIntLE();
                checkpoint(ConnectInputState.USERID_AND_NULL);
                byte[] ipArray = new byte[4];
                Conversion.intToByteArray(ip, 0, new byte[4], 0, 4);
                InetAddress inetAddress = InetAddress.getByAddress(ipArray);
                socksIP4Connect.setDestIp(ip);
                socksIP4Connect.setDestAddress(inetAddress);
                baos = new ByteArrayOutputStream();

            case USERID_AND_NULL:
                byte b = 0;
                while ((b = in.readByte()) != 0) {
                    baos.write(b);
                }

                String userId = new String(baos.toByteArray());
                socksIP4Connect.setUserId(userId);
                baos = null;

                ctx.pipeline().remove(this);
                ctx.writeAndFlush(socksIP4Connect);
                break;

            case VERSION5_NAUTH:
                byte numberOfAuthMethods = in.readByte();
                byte[] bytes = new byte[numberOfAuthMethods];
                in.readBytes(bytes);
                if (isNoAuthRequested(bytes)) {
                    ctx.writeAndFlush(new Socks5AcceptedAuth(SocksConst.AUTH_NO_AUTH));
                    checkpoint(ConnectInputState.VERSION5_CLIENT_AUTH);
                } else {
                    ctx.writeAndFlush(new Socks5AuthNotSupported());
                }
                return;

            case VERSION5_CLIENT_AUTH:
                in.readByte();
                byte cmd = in.readByte();
                if (cmd == SocksConst.COMMAND_ESTABLISH_TCP) {
                    in.readByte(); // ignore, reserved 0
                    byte addrType = in.readByte();
                    if (addrType == SocksConst.IPV4) {
                        int ipDest = in.readIntLE();
                        byte[] a = Conversion.intToByteArray(ipDest, 0, new byte[4], 0, 4);
                        InetAddress inetAddressDest = InetAddress.getByAddress(a);
                        socksIP4Connect = new SocksIP4ConnectRequest(SocksConst.VER5);
                        socksIP4Connect.setDestIp(ipDest);
                        socksIP4Connect.setDestAddress(inetAddressDest);
                        socksIP4Connect.setPort(in.readShort());
                        ctx.pipeline().remove(this);
                        ctx.writeAndFlush(socksIP4Connect);
                    } if (addrType == SocksConst.DOMAIN) {
                        byte domainLength = in.readByte();
                        byte[] domainName = new byte[domainLength];
                        in.readBytes(domainName);
                        InetAddress domain = InetAddress.getByName(new String(domainName, StandardCharsets.ISO_8859_1));
                        socksIP4Connect = new SocksIP4ConnectRequest(SocksConst.VER5);
                        socksIP4Connect.setDestAddress(domain);
                        socksIP4Connect.setPort(in.readShort());

                        ctx.pipeline().remove(this);
                        ctx.writeAndFlush(socksIP4Connect);
                    }
                } else  {
                    ctx.pipeline().remove(this);
                    ctx.writeAndFlush(new SocksCloseConnection());
                }
        }
    }

    private boolean isNoAuthRequested(byte[] auths) {
        for (int i = 0; i < auths.length; i++) {
            if (auths[i] == SocksConst.AUTH_NO_AUTH) {
                return true;
            }
        }

        return false;
    }
}

