package com.github.marcin84s;

import com.github.marcin84s.consts.SocksConst;
import com.github.marcin84s.netty.Netty;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class SocksConnectTest {

    @Rule
    public final WireMockRule wiremock = new WireMockRule(8080);

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        Netty.bindSocks4Proxy(4321).sync();
    }

    @AfterClass
    public static void afterClass() {
        Netty.shutdownWorker();
    }

    @Test
    public void socks4connectShouldWork() throws InterruptedException, IOException {
        // GIVEN
        stubFor(get(urlEqualTo("/testSocks4"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Success")));

        System.setProperty("socksProxyVersion", "4");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 4321));

        // WHEN
        URL url = new URL("http://localhost:8080/testSocks4");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
        httpURLConnection.connect();

        int responseCode = httpURLConnection.getResponseCode();
        byte[] bytes = httpURLConnection.getInputStream().readAllBytes();
        String responseBody = new String(bytes);
        httpURLConnection.disconnect();

        // THEN
        assertEquals(200, responseCode);
        assertEquals("Success", responseBody);
    }

    @Test
    public void socks5connectShouldWork() throws InterruptedException, IOException {
        // GIVEN
        stubFor(get(urlEqualTo("/testSocks5"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Success")));

        System.setProperty("socksProxyVersion", "5");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 4321));

        // WHEN
        URL url = new URL("http://localhost:8080/testSocks5");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
        httpURLConnection.connect();

        int responseCode = httpURLConnection.getResponseCode();
        byte[] bytes = httpURLConnection.getInputStream().readAllBytes();
        String responseBody = new String(bytes);
        httpURLConnection.disconnect();

        // THEN
        assertEquals(200, responseCode);
        assertEquals("Success", responseBody);
    }
}
