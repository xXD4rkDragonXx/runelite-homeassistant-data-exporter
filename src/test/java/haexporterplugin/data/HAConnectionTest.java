package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class HAConnectionTest {

    @Test
    public void testConstructorAndGetters() {
        HAConnection connection = new HAConnection("http://homeassistant.local:8123", "abc123token");
        assertEquals("http://homeassistant.local:8123", connection.getBaseUrl());
        assertEquals("abc123token", connection.getToken());
    }

    @Test
    public void testWithHttpsUrl() {
        HAConnection connection = new HAConnection("https://ha.example.com", "secure-token-xyz");
        assertEquals("https://ha.example.com", connection.getBaseUrl());
        assertEquals("secure-token-xyz", connection.getToken());
    }

    @Test
    public void testWithIpAddress() {
        HAConnection connection = new HAConnection("http://192.168.1.100:8123", "token-123");
        assertEquals("http://192.168.1.100:8123", connection.getBaseUrl());
    }

    @Test
    public void testWithEmptyValues() {
        HAConnection connection = new HAConnection("", "");
        assertEquals("", connection.getBaseUrl());
        assertEquals("", connection.getToken());
    }

    @Test
    public void testWithNullValues() {
        HAConnection connection = new HAConnection(null, null);
        assertNull(connection.getBaseUrl());
        assertNull(connection.getToken());
    }
}
