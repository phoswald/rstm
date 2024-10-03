package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.auth;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.login;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.SimpleIdentityProvider;
 
class HttpServerWithAuthTest {
    
    private static final IdentityProvider identityProvider = new SimpleIdentityProvider() //
            .add("username1", "password1", List.of("role1", "role3")) //
            .add("username2", "password2", List.of("role2"));

    private final Principal username1 = identityProvider.authenticate("username1", "password1".toCharArray()).get();
    private final Principal username2 = identityProvider.authenticate("username2", "password2".toCharArray()).get();
    
    private static final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .filter(combine( //
                    route("/login", login()), //
                    route("/secured", auth("role1", //
                            route("/resource", get(request -> //
                                    HttpResponse.text(200, "Hello, " + request.principal().name() + "!"))))) //
            )) //
            .identityProvider(identityProvider) //
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
        given().
            formParam("username", "username1").
            formParam("password", "password1").
        when().
            post("/login").
        then().
            statusCode(302).
            header("location", ".").
            cookie("session", username1.token());
    }
    
    @Test
    void post_login_denied() {
        given().
            redirects().follow(false).
        when().
            post("/login").
        then().
            statusCode(302).
            header("location", "login-error.html").
            cookies(Map.of());
    }

    @Test
    void get_noAuth_redirect() {
        given().
            redirects().follow(false).
        when().
            get("/secured/resource").
        then().
            statusCode(302).
            header("location", "../login.html");
    }

    @Test
    void get_basicAuth_allowed() {
        given().
            auth().preemptive().basic("username1", "password1").
        when().
            get("/secured/resource").
        then().
            statusCode(200).
            body(equalTo("Hello, username1!"));
    }

    @Test
    void get_basicAuth_denied() {
        given().
            auth().preemptive().basic("username2", "password2").
        when().
            get("/secured/resource").
        then().
            statusCode(401);
    }

    @Test
    void get_basicAuth_redirect() {
        given().
            redirects().follow(false).
            auth().preemptive().basic("username1", "bad").
        when().
            get("/secured/resource").
        then().
            statusCode(302).
            header("location", "../login.html");
    }

    @Test
    void get_bearerAuth_allowed() {
        given().
            auth().preemptive().oauth2(username1.token()).
        when().
            get("/secured/resource").
        then().
            statusCode(200).
            body(equalTo("Hello, username1!"));
    }

    @Test
    void get_bearerAuth_denied() {
        given().
            auth().preemptive().oauth2(username2.token()).
        when().
            get("/secured/resource").
        then().
            statusCode(401);
    }

    @Test
    void get_bearerAuth_redirect() {
        given().
            redirects().follow(false).
            auth().preemptive().oauth2("bad").
        when().
            get("/secured/resource").
        then().
            statusCode(302).
            header("location", "../login.html");
    }

    @Test
    void get_session_allowed() {
        given().
            cookie("session", username1.token()).
        when().
            get("/secured/resource").
        then().
            statusCode(200).
            body(equalTo("Hello, username1!"));
    }

    @Test
    void get_session_denied() {
        given().
            cookie("session", username2.token()).
        when().
            get("/secured/resource").
        then().
            statusCode(401);
    }

    @Test
    void get_session_redirect() {
        given().
            redirects().follow(false).
            cookie("session", "bad").
        when().
            get("/secured/resource").
        then().
            statusCode(302).
            header("location", "../login.html");
    }
}
