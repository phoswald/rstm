package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.auth;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.login;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.oidc;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.OidcRedirect;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.SimpleIdentityProvider;

class HttpServerWithAuthTest {

    private static final IdentityProvider localIdentityProvider = new SimpleIdentityProvider()
            .withUser("username1", "password1", List.of("role1", "role3"))
            .withUser("username2", "password2", List.of("role2"));

    /**
     * Local users plus a fake OIDC provider "idp1" whose state token simply echoes the state.
     */
    private static final IdentityProvider identityProvider = new IdentityProvider() {

        @Override
        public Optional<Principal> authenticateWithPassword(String username, char[] password) {
            return localIdentityProvider.authenticateWithPassword(username, password);
        }

        @Override
        public Optional<Principal> authenticateWithToken(String token) {
            return localIdentityProvider.authenticateWithToken(token);
        }

        @Override
        public Optional<OidcRedirect> authenticateWithOidcRedirect(String provider) {
            if (!provider.equals("idp1")) {
                return Optional.empty();
            }
            return Optional.of(new OidcRedirect("https://idp1.example.com/auth?state=state1", "token-state1"));
        }

        @Override
        public Optional<Principal> authenticateWithOidcCallback(String code, String state, String stateToken) {
            if (!code.equals("code1") || !("token-" + state).equals(stateToken)) {
                return Optional.empty();
            }
            return localIdentityProvider.authenticateWithPassword("username1", "password1".toCharArray());
        }
    };

    private final Principal username1 = identityProvider.authenticateWithPassword("username1", "password1".toCharArray()).get();
    private final Principal username2 = identityProvider.authenticateWithPassword("username2", "password2".toCharArray()).get();

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/login", login()),
                    route("/oidc", oidc()),
                    route("/secured", auth("role1",
                            route("/resource", get(request ->
                                    HttpResponse.text(200, "Hello, " + request.principal().name() + "!")))))
            ))
            .identityProvider(identityProvider)
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void token_format() {
        assertThat(username1.token(), matchesRegex("[0-9a-f]{32}"));
    }

    @Test
    void post_login_allowed() {
        given()
                .formParam("username", "username1")
                .formParam("password", "password1")
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", ".")
                .cookie("session", username1.token());
    }

    @Test
    void post_login_denied() {
        given()
                .redirects().follow(false)
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", "login-error.html")
                .cookies(Map.of());
    }

    @Test
    void get_noAuth_redirect() {
        given()
                .redirects().follow(false)
                .when()
                .get("/secured/resource?x=1")
                .then()
                .statusCode(302)
                .header("location", "../login.html")
                .cookie("login_return", detailedCookie()
                        .value("%2Fsecured%2Fresource%3Fx%3D1")
                        .httpOnly(true)
                        .path("/")
                        .sameSite("lax")
                        .maxAge(-1));
    }

    @Test
    void post_noAuth_redirectWithoutReturn() {
        given()
                .redirects().follow(false)
                .when()
                .post("/secured/resource")
                .then()
                .statusCode(302)
                .header("location", "../login.html")
                .cookies(Map.of());
    }

    @Test
    void post_login_withReturn() {
        given()
                .redirects().follow(false)
                .cookie("login_return", "%2Fsecured%2Fresource%3Fx%3D1")
                .formParam("username", "username1")
                .formParam("password", "password1")
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", "secured/resource?x=1")
                .cookie("session", username1.token())
                .cookie("login_return", detailedCookie().value("").maxAge(0));
    }

    @Test
    void post_login_deniedKeepsReturn() {
        given()
                .redirects().follow(false)
                .cookie("login_return", "%2Fsecured%2Fresource")
                .formParam("username", "username1")
                .formParam("password", "bad")
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", "login-error.html")
                .cookies(Map.of());
    }

    @Test
    void post_login_protocolRelativeReturn() {
        given()
                .redirects().follow(false)
                .cookie("login_return", "%2F%2Fevil.example.com%2Fx")
                .formParam("username", "username1")
                .formParam("password", "password1")
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", ".");
    }

    @Test
    void post_login_schemeLikeReturn() {
        given()
                .redirects().follow(false)
                .cookie("login_return", "%2Fhttps%3Aevil.example.com")
                .formParam("username", "username1")
                .formParam("password", "password1")
                .when()
                .post("/login")
                .then()
                .statusCode(302)
                .header("location", "./https:evil.example.com");
    }

    @Test
    void get_login_oidcRedirect() {
        given()
                .redirects().follow(false)
                .queryParam("provider", "idp1")
                .when()
                .get("/login")
                .then()
                .statusCode(302)
                .header("location", "https://idp1.example.com/auth?state=state1")
                .cookie("login_state", detailedCookie()
                        .value("token-state1")
                        .httpOnly(true)
                        .path("/")
                        .sameSite("lax")
                        .maxAge(300));
    }

    @Test
    void get_oidc_callbackAllowed() {
        given()
                .redirects().follow(false)
                .cookie("login_state", "token-state1")
                .cookie("login_return", "%2Fsecured%2Fresource")
                .queryParam("code", "code1")
                .queryParam("state", "state1")
                .when()
                .get("/oidc")
                .then()
                .statusCode(302)
                .header("location", "secured/resource")
                .cookie("session", username1.token())
                .cookie("login_state", detailedCookie().value("").maxAge(0))
                .cookie("login_return", detailedCookie().value("").maxAge(0));
    }

    @Test
    void get_oidc_callbackWithoutStateCookieDenied() {
        given()
                .redirects().follow(false)
                .queryParam("code", "code1")
                .queryParam("state", "state1")
                .when()
                .get("/oidc")
                .then()
                .statusCode(302)
                .header("location", "login-error.html")
                .cookie("login_state", detailedCookie().value("").maxAge(0));
    }

    @Test
    void get_basicAuth_allowed() {
        given()
                .auth().preemptive().basic("username1", "password1")
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, username1!"));
    }

    @Test
    void get_basicAuth_denied() {
        given()
                .auth().preemptive().basic("username2", "password2")
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(403);
    }

    @Test
    void get_basicAuth_redirect() {
        given()
                .redirects().follow(false)
                .auth().preemptive().basic("username1", "bad")
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(302)
                .header("location", "../login.html");
    }

    @Test
    void get_bearerAuth_allowed() {
        given()
                .auth().preemptive().oauth2(username1.token())
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, username1!"));
    }

    @Test
    void get_bearerAuth_denied() {
        given()
                .auth().preemptive().oauth2(username2.token())
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(403);
    }

    @Test
    void get_bearerAuth_redirect() {
        given()
                .redirects().follow(false)
                .auth().preemptive().oauth2("bad")
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(302)
                .header("location", "../login.html");
    }

    @Test
    void get_session_allowed() {
        given()
                .cookie("session", username1.token())
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, username1!"));
    }

    @Test
    void get_session_denied() {
        given()
                .cookie("session", username2.token())
                .when()
                .get("/secured/resource")
                .then()
                .statusCode(403);
    }

    @Test
    void get_session_redirect() {
        given()
                .redirects().follow(false)
                .cookie("session", "bad")
                .when().get("/secured/resource")
                .then()
                .statusCode(302)
                .header("location", "../login.html");
    }
}
