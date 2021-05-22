package com.github.marcin84s.consts;

public class SocksConst {
    public static final byte COMMAND_CONNECT = 1;
    public static final byte AUTH_NO_AUTH = 0;
    public static final byte VER4 = 4;
    public static final byte VER5 = 5;
    public static final byte AUTH_NOT_SUPPORTED = (byte) 0xff;
    public static final byte COMMAND_ESTABLISH_TCP = 1;
    public static final byte IPV4 = 1;
    public static final byte DOMAIN = 3;
    public static final byte STATUS_GRANTED = 0;
    public static final byte STATUS_GENERAL_FAILURE = 1;

    public static final byte REPLY_COMMAND_GRANTED = (byte) 0x5A;
    public static final byte REPLY_COMMAND_REJECTED = (byte) 0x5B;
}
