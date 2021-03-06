package com.github.marcin84s.handler.msgs;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.net.InetAddress;

public class SocksIP4ConnectRequest {
    private final int version;
    private int destIp;
    private int port;
    private String userId;
    private InetAddress destAddress;
    public SocksIP4ConnectRequest(int version) {
        this.version = version;
    }

    public int getDestIp() {
        return destIp;
    }

    public void setDestIp(int destIp) {
        this.destIp = destIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public InetAddress getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(InetAddress destAddress) {
        this.destAddress = destAddress;
    }

    public String getEndpoint() {
        return destAddress.toString() + ":" + port;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("version", version)
                .append("destAddress", destAddress)
                .append("port", port)
                .append("userId", userId)
                .toString();
    }
}



