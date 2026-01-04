package com.github.phoswald.rstm.security.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordUtilityTest {

    @ParameterizedTest
    @MethodSource("validPasswordPairs")
    void hashPassword_valid_success(String password) {
        String hash = PasswordUtility.hashPassword(password.toCharArray());
        assertThat(hash, matchesRegex("\\$2a\\$10\\$[A-Za-z0-9./]{53}"));
        assertTrue(PasswordUtility.verifyPassword(password.toCharArray(), hash));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1234567890123456789012345678901234567890123456789012345678901234567890123" })
    @NullSource
    void hashPassword_invalid_exception(String password) {
        assertThrows(RuntimeException.class, () -> PasswordUtility.hashPassword(password.toCharArray()));
    }

    @ParameterizedTest
    @MethodSource("validPasswordPairs")
    void verifyPassword_valid_success(String password, String hash) {
        assertTrue(PasswordUtility.verifyPassword(password.toCharArray(), hash));
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordPairs")
    void verifyPassword_invalid_failure(String password, String hash) {
        assertFalse(PasswordUtility.verifyPassword(password.toCharArray(), hash));
    }

    private static Stream<Arguments> validPasswordPairs() {
        return Stream.of(
                // Format '2a' (Quarkus, Bcrypt Java Library)
                Arguments.of("", "$2a$10$i3/AqMG06amV.0pAzbrlie5QPJ3lzHcs9y1HmFPydu2MqmllIA3cO"),
                Arguments.of("password1", "$2a$10$mOJfZGme6tolM2tjFtb.CeMvmFKX.tuwwCb05vDm1N9N144r.PrLe"),
                Arguments.of("1234", "$2a$10$6Y5icwd4O084z6Ob6DJ3BO60vSl06ijJOZnNtsw7BThWDXwqDHVt2"),
                Arguments.of("12345678", "$2a$10$z/8csozgyZGfZ5Fd0.9yeOcPPPVdI.ruZhMQfyQJBRpruvMHYZzF."),
                Arguments.of("123456789", "$2a$10$VroUnFCTYcg2cpYolGNz5eGpEjhLmtBcz2WCJrtmjt5tFfM71GSB."),
                Arguments.of("€ äöü αβγδ กขฃ", "$2a$10$BUFCDrJW/.GMQFVpi2irFe1ykBvOZt1h/wExw9oJppNt6FuL.gOsm"),
                // Format '2y' (Apache htpasswd)
                Arguments.of("12345678", "$2y$05$hStX/BAVsWwjbpri1CQtAeBRlaERs09MYZjLqz8SGi6.hi0L5Y17O")
        );
    }

    private static Stream<Arguments> invalidPasswordPairs() {
        return Stream.of(
                Arguments.of("1235", "$2a$10$6Y5icwd4O084z6Ob6DJ3BO60vSl06ijJOZnNtsw7BThWDXwqDHVt2"), // wrong password
                Arguments.of("1234", "$2a$11$5Y5icwd4O084z6Ob6DJ3BO60vSl06ijJOZnNtsw7BThWDXwqDHVt2"), // wrong rounds
                Arguments.of("1234", "$2a$10$5Y5icwd4O084z6Ob6DJ3BO60vSl06ijJOZnNtsw7BThWDXwqDHVt2"), // change at beginning of salt
                Arguments.of("1234", "$2a$10$6Y5icwd4O084z6Ob6DJ3BO60vSl06ijJOZnNtsw7BThWDXwqDHVt1"), // change at end of password
                Arguments.of("1234", "bad") // wrong format
        );
    }
}
