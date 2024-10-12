package com.github.phoswald.rstm.security.oidc;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.RegexBody.regex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import com.github.phoswald.rstm.security.Principal;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8080)
class OidcUtilTest {

    private final OidcUtil testee = new OidcUtil("https://example.com/oauth/callback") //
            .addDex("rstm-app", "rstm-secret", "http://localhost:8080/dex") //
            .addGoogle("some-app.apps.googleusercontent.com", "some-secret");

    // for troubleshooting: set org.mockserver.log.MockServerEventLog from "warn" to "info"
    private final ClientAndServer cas;

    public OidcUtilTest(ClientAndServer cas) {
        this.cas = cas;
    }

    @Test
    void authorize_dex_success() throws IOException {
        mockDexConfig();
        mockDexKeySet();
        
        Optional<String> url = testee.authorize("dex");
        assertThat(url.get(), startsWith("http://127.0.0.1:5556/dex/auth?"));
        assertThat(url.get(), containsString("?response_type=code&"));
        assertThat(url.get(), containsString("&client_id=rstm-app&"));
        assertThat(url.get(), containsString("&redirect_uri=https%3A%2F%2Fexample.com%2Foauth%2Fcallback&"));
        assertThat(url.get(), containsString("&scope=openid+profile+email+offline_access&"));
        assertThat(url.get(), matchesRegex(".*&state=[0-9a-f]{32}$"));
    }

    @Test
    void authorize_google_success() {
        Optional<String> url = testee.authorize("google");
        assertThat(url.get(), startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertThat(url.get(), containsString("?response_type=code&"));
        assertThat(url.get(), containsString("&client_id=some-app.apps.googleusercontent.com&"));
        assertThat(url.get(), containsString("&redirect_uri=https%3A%2F%2Fexample.com%2Foauth%2Fcallback&"));
        assertThat(url.get(), containsString("&scope=openid+profile+email&"));
        assertThat(url.get(), matchesRegex(".*&state=[0-9a-f]{32}$"));
    }
    
    @Test
    void callback_dex() throws IOException {
        mockDexConfig();
        mockDexKeySet();
        mockDexTokenExchange("rstm-app", "rstm-secret", "code1");
        String state = createState();
        
        Optional<Principal> principal = testee.callback("code1", state);
        assertTrue(principal.isPresent());
        assertEquals("kilgore@kilgore.trout", principal.get().name());
        assertEquals(List.of(), principal.get().roles());        
    }
    
    private String createState() {
        String location = testee.authorize("dex").get();
        Matcher matcher = Pattern.compile("state=([0-9a-f]{32})").matcher(location);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void mockDexConfig() throws IOException {
        cas.when(request() //
                .withMethod("GET") //
                .withPath("/dex/.well-known/openid-configuration") //
        ).respond(response() //
                .withStatusCode(200) //
                .withHeader("Content-Type", "application/json") //
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-config.json"))));
    }
    
    private void mockDexKeySet() throws IOException {
        cas.when(request() //
                .withMethod("GET") //
                .withPath("/dex/keys") //
        ).respond(response() //
                .withStatusCode(200) //
                .withHeader("Content-Type", "application/json") //
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-keys.json"))));
    }
    
    private void mockDexTokenExchange(String clientId, String clientSecret, String code) throws IOException {
        cas.when(request() //
                .withMethod("POST") //
                .withPath("/dex/token") //
                .withContentType(MediaType.APPLICATION_FORM_URLENCODED)
                .withBody(regex(".*code=" + code + "&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=https%3A%2F%2Fexample.com%2Foauth%2Fcallback.*"))
        ).respond(response() //
                .withStatusCode(200) //
                .withHeader("Content-Type", "application/json") //
                .withBody(Files.readString(Path.of("src/test/resources/mock-server/dex-token.json"))));
    }
}
