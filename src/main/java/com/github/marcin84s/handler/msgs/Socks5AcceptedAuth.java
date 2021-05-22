package com.github.marcin84s.handler.msgs;

public class Socks5AcceptedAuth {
    private final byte auth;

    public Socks5AcceptedAuth(byte auth) {
        this.auth = auth;
    }

    public byte getAuth() {
        return auth;
    }
}
