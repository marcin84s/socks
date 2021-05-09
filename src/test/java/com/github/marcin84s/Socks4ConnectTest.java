package com.github.marcin84s;

import com.github.marcin84s.netty.Netty;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class Socks4ConnectTest {

    @Rule
    public final WireMockRule wiremock = new WireMockRule(8080);

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        Netty.createSocks4Proxy(4321).sync();
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

        // THEN
        assertEquals(200, responseCode);
        assertEquals("Success", responseBody);
    }

    @Test
    public void socks4connectShouldWorkAfterDowngradeFrom5() throws InterruptedException, IOException {
        // GIVEN
        stubFor(get(urlEqualTo("/testSocks4Downgrade"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Success")));

        // WHEN
        System.clearProperty("socksProxyVersion");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 4321));

        URL url = new URL("http://localhost:8080/testSocks4Downgrade");
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
