package com.github.marcin84s;

import com.github.marcin84s.handler.Socks4ConnectRequestDecoder;
import com.github.marcin84s.handler.Socks4ConnectResponseHandler;
import com.github.marcin84s.netty.Netty;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertEquals;

public class Socks4ConnectTest {

    @Rule
    public final WireMockRule wiremock = new WireMockRule(8080);

    @Test
    public void socks4connectShouldWork() throws InterruptedException, IOException {
        // GIVEN
        stubFor(get(urlEqualTo("/testSocks4"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Success")));

        Netty.createSocks4Proxy(4321).sync();

        // WHEN
        System.setProperty("socksProxyVersion", "4");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 4321));

        URL url = new URL("http://localhost:8080/testSocks4");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
        httpURLConnection.connect();

        int responseCode = httpURLConnection.getResponseCode();
        byte[] bytes = httpURLConnection.getInputStream().readAllBytes();
        String responseBody = new String(bytes);
        httpURLConnection.disconnect();
        Netty.shutdownWorker();

        // THEN
        assertEquals(200, responseCode);
        assertEquals("Success", responseBody);
    }
}
