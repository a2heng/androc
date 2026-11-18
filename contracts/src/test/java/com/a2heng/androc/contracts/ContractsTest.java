package com.a2heng.androc.contracts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ContractsTest {
    @Test
    public void connectionNormalizesBaseUrlAndDefaultsUsername() {
        RuntimeConnection c = RuntimeConnection.basic("http://127.0.0.1:4096///", "secret");
        assertEquals("http://127.0.0.1:4096", c.baseUrl());
        assertEquals("opencode", c.username());
        assertEquals("secret", c.password());
        assertTrue(c.hasPassword());
        assertEquals("Basic b3BlbmNvZGU6c2VjcmV0", c.authorizationHeader());
    }

    @Test
    public void pairingLinkIsStandardOpenCode() {
        assertEquals(
                "http://127.0.0.1:4096/auth/connect/abc",
                Pairing.link("http://127.0.0.1:4096/", "abc"));
        assertEquals(
                "http://127.0.0.1:4096/auth/connect/abc",
                RuntimeConnection.basic("http://127.0.0.1:4096", "x").pairingLink("abc"));
    }

    @Test
    public void qrPayloadCarriesCodeAndReachableUrls() {
        assertEquals(
                "{\"code\":\"a\\\"b\",\"urls\":[\"http://10.0.0.2:4096\"]}",
                Pairing.qrPayload("a\"b", "http://10.0.0.2:4096"));
        assertFalse(Pairing.qrPayload("abc", (String) null).contains("null"));
    }

    @Test
    public void discoveryUsesApplicationScopedNames() {
        assertEquals("com.a2heng.androc.local-server",
                DiscoveryContract.authority("com.a2heng.androc"));
        assertEquals("com.a2heng.androc.permission.LOCAL_SERVER_INFO",
                DiscoveryContract.permission("com.a2heng.androc"));
        assertEquals("content://com.a2heng.androc.local-server/info",
                DiscoveryContract.uri("com.a2heng.androc"));
    }

    @Test
    public void statePredicates() {
        assertTrue(RuntimeState.READY.isReady());
        assertTrue(RuntimeState.STARTING.isRunning());
        assertFalse(RuntimeState.STOPPED.isRunning());
        assertFalse(RuntimeState.FAILED.isReady());
    }
}
