package com.github.phoswald.rstm.security.oidc;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.SimpleIdentityProvider;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8080)
class OidcIdentityProviderTest {

    private static final String TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImQ1OTgzNGNlOTc3YmVkNDZkNmE1NGI0NjZjZjdiODk3NzBhOWZiOTIifQ.eyJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjU1NTYvZGV4Iiwic3ViIjoiQ2cwd0xUTTROUzB5T0RBNE9TMHdFZ1J0YjJOciIsImF1ZCI6InJzdG0tYXBwIiwiZXhwIjoxNzI4ODMzMDQzLCJpYXQiOjE3Mjg3NDY2NDMsImF0X2hhc2giOiJPRmowVHg1MHRTOTVReERZb1hFZnVnIiwiY19oYXNoIjoielNRbEEyWlprQUxIWW9jcklIQ0F3ZyIsImVtYWlsIjoia2lsZ29yZUBraWxnb3JlLnRyb3V0IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJLaWxnb3JlIFRyb3V0In0.py5M1sFUHZVhQJjooI299tdAWBsdLaZebD7j2TpBNCsiWldcrJtV8HRejADrpfuNX-XvqytbLATYbSQ1AMV8J95mfaFvkbrWAUFY2MXF81F2Fzp1xXAdRL-PlwkYsUiWanfOiJE3EZFr-EQURvCng9VW9wO_mqvRpf7NjCDvYa0kk_H5ntGhoH76dSRQH4gY_5-58SfiPfQdEZSzg0khQzyLlD5jJuxTo6HKrh3BkyPYsgRszX1C8sKKuckDPbuPKKLkRNMwA48fuNrFeEhI8RrByrCTPjZtFGFlFiktZijXJ2T7ji6-mieyXfSK8rZSxHzCueauU0hqoAKa6WHSZg";
    private static final String DEX_PROVIDER = "dex";
    private static final String REDIRECT_URI = "https://example.com/oauth/callback";

    // for troubleshooting: set org.mockserver.log.MockServerEventLog from "warn" to "info"
    private final ClientAndServer mockServer;
    private final IdentityProvider upstream = new SimpleIdentityProvider().withUser("username1", "password1", List.of("role1"));
    private final Instant now = Instant.ofEpochSecond(1727968220); // 2024-10-03 15:10:20 UTC
    private long random = 1; // RandomGenerator produces a sequence of longs
    private IdentityProvider testee;

    OidcIdentityProviderTest(ClientAndServer mockServer) {
        this.mockServer = mockServer;
    }

    @BeforeEach
    void prepare() throws IOException {
        mockDexConfig();
        mockDexKeySet();
        mockDexTokenExchange("rstm-app", "rstm-secret", "code1");
        testee = new OidcIdentityProvider(REDIRECT_URI, upstream, () -> now, () -> random++)
                .withDex("rstm-app", "rstm-secret", "http://localhost:8080/dex")
                .withGoogle("some-app.apps.googleusercontent.com", "some-secret");
    }

    @Test
    void authenticateWithOidcRedirect_dex_success() {
        Optional<String> url = testee.authenticateWithOidcRedirect(DEX_PROVIDER);
        assertTrue(url.isPresent());
        assertThat(url.get(), startsWith("http://127.0.0.1:5556/dex/auth?"));
        assertThat(url.get(), containsString("?response_type=code&"));
        assertThat(url.get(), containsString("&client_id=rstm-app&"));
        assertThat(url.get(), containsString("&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, UTF_8) + "&"));
        assertThat(url.get(), containsString("&scope=openid+profile+email+offline_access&"));
        assertThat(url.get(), endsWith("&state=01000000000000000200000000000000"));
    }

    @Test
    void authenticateWithOidcRedirect_google_success() {
        Optional<String> url = testee.authenticateWithOidcRedirect("google");
        assertTrue(url.isPresent());
        assertThat(url.get(), startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertThat(url.get(), containsString("?response_type=code&"));
        assertThat(url.get(), containsString("&client_id=some-app.apps.googleusercontent.com&"));
        assertThat(url.get(), containsString("&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, UTF_8) + "&"));
        assertThat(url.get(), containsString("&scope=openid+profile+email&"));
        assertThat(url.get(), endsWith("&state=01000000000000000200000000000000"));
    }

    @Test
    void authenticateWithOidcCallback_dex_success() {
        Optional<String> url = testee.authenticateWithOidcRedirect(DEX_PROVIDER);
        assertTrue(url.isPresent());
        assertThat(url.get(), endsWith("&state=01000000000000000200000000000000"));

        Optional<Principal> principal = testee.authenticateWithOidcCallback("code1", "01000000000000000200000000000000");
        assertTrue(principal.isPresent());
        assertEquals("kilgore@kilgore.trout", principal.get().name());
        assertEquals(List.of("user"), principal.get().roles());
        assertEquals(DEX_PROVIDER, principal.get().provider());
        assertEquals(TOKEN, principal.get().token());
    }

    @Test
    void authenticateWithToken_valid_success() {
        Optional<Principal> principal = testee.authenticateWithToken(TOKEN);
        assertTrue(principal.isPresent());
        assertEquals("kilgore@kilgore.trout", principal.get().name());
        assertEquals(List.of("user"), principal.get().roles());
        assertEquals(DEX_PROVIDER, principal.get().provider());
        assertEquals(TOKEN, principal.get().token());
    }

    @Test
    void authenticateWithPassword_validUpstream_success() {
        Principal principal = testee.authenticateWithPassword("username1", "password1".toCharArray()).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1"), principal.roles());
        assertEquals(LOCAL_PROVIDER, principal.provider());
        assertThat(principal.token(), matchesRegex("[0-9a-f]{32}"));
    }

    @Test
    void authenticateWithToken_validUpstream_success() {
        String token = upstream.authenticateWithPassword("username1", "password1".toCharArray()).get().token();

        Principal principal = testee.authenticateWithToken(token).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1"), principal.roles());
        assertEquals(LOCAL_PROVIDER, principal.provider());
        assertThat(principal.token(), matchesRegex("[0-9a-f]{32}"));
    }

    private void mockDexConfig() throws IOException {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/dex/.well-known/openid-configuration")
        ).respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-config.json"))));
    }

    private void mockDexKeySet() throws IOException {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/dex/keys")
        ).respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-keys.json"))));
    }

    private void mockDexTokenExchange(String clientId, String clientSecret, String code) throws IOException {
        mockServer.when(request()
                .withMethod("POST")
                .withPath("/dex/token")
                .withContentType(MediaType.APPLICATION_FORM_URLENCODED)
                .withBody("grant_type=authorization_code&code=" + code + "&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, UTF_8))
        ).respond(response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-token.json"))));
    }
}
