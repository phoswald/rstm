package com.github.phoswald.rstm.security.password;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Hashing and Verifying passwords using bcrypt
///
/// The hashing algorithm is consistent with:
/// - `io.quarkus.elytron.security.common.BcryptUtil.bcryptHash(...)`, which defaults to cost 10,
///   from `io.quarkus:quarkus-elytron-security-common:3.30.5`
/// - `at.favre.lib.crypto.bcrypt.BCrypt().with(VERSION_2A).hashToString(10, ...)`
///   from `at.favre.lib:bcrypt:0.10.2`
///
/// See also:
/// - https://github.com/patrickfav/bcrypt
/// - https://en.wikipedia.org/wiki/Bcrypt
public class PasswordUtility {

    private static final Pattern PATTERN = Pattern.compile("\\$(2a|2y)\\$([0-9]+)\\$([A-Za-z0-9./]{53})");

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtility.class);

    public static void main(String[] args) {
        char[] password1 = System.console().readPassword("Enter password: ");
        char[] password2 = System.console().readPassword("Repeat password: ");
        if(Arrays.equals(password1, password2)) {
            System.out.println("Hashed password: " + hashPassword(password1));
        } else {
            System.err.println("Passwords do not match!");
        }
    }

    public static String hashPassword(char[] password) {
        return formatHashedPassword(new BCrypt().hashPassword(password));
    }

    public static boolean verifyPassword(char[] enteredPassword, String hashedPassword) {
        try {
            return new BCrypt().verifyPassword(enteredPassword, parseHashedPassword(hashedPassword));
        } catch(RuntimeException e) {
            logger.warn("Failed to verify password: {}", e.getMessage());
            return false;
        }
    }

    private static String formatHashedPassword(HashedPassword hash) {
        StringBuilder buffer = new StringBuilder();
        buffer.append('$');
        buffer.append(hash.version());
        buffer.append('$');
        buffer.append(String.format(Locale.US, "%02d", hash.cost()));
        buffer.append('$');
        buffer.append(CustomBase64.encode(hash.salt()));
        buffer.append(CustomBase64.encode(hash.hash()));
        return buffer.toString();
    }

    private static HashedPassword parseHashedPassword(String hashedPassword) {
        Matcher matcher = PATTERN.matcher(hashedPassword);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("Invalid hashed password: expected pattern is " + PATTERN);
        }
        String version = matcher.group(1);
        int cost = Integer.parseInt(matcher.group(2));
        int saltLength = BCrypt.SALT_LENGTH;
        String saltPart = matcher.group(3).substring(0, CustomBase64.encodedSize(saltLength));
        String hashPart = matcher.group(3).substring(CustomBase64.encodedSize(saltLength));
        byte[] salt = CustomBase64.decode(saltPart.toCharArray());
        byte[] hash = CustomBase64.decode(hashPart.toCharArray());
        return new HashedPassword(version, cost, salt, hash);
    }

    private static class BCrypt {

        private static final Charset CHARSET = UTF_8;
        private static final int SALT_LENGTH = 16;
        private static final int HASH_LENGTH = 23;

        private final String version = "2a";
        private final int cost = 10;
        private final SecureRandom random = new SecureRandom();

        HashedPassword hashPassword(char[] password) {
            byte[] salt = randomBytes(SALT_LENGTH);
            byte[] hash = hash(cost, salt, encodePassword(password, true));
            return new HashedPassword(version, cost, salt, Arrays.copyOf(hash, HASH_LENGTH));
        }

        boolean verifyPassword(char[] enteredPassword, HashedPassword hashedPassword) {
            byte[] hash = hash(hashedPassword.cost(), hashedPassword.salt(), encodePassword(enteredPassword, true));
            return Arrays.equals(hashedPassword.hash(), Arrays.copyOf(hash, HASH_LENGTH));
        }

        private byte[] randomBytes(int count) {
            byte[] buffer = new byte[count];
            random.nextBytes(buffer);
            return buffer;
        }

        private byte[] encodePassword(char[] password, boolean appendNull) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try(Writer stream = new OutputStreamWriter(buffer, CHARSET)) {
                stream.write(password);
                if(appendNull) {
                    stream.write('\0');
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return buffer.toByteArray();
        }

        private byte[] hash(int cost, byte[] salt, byte[] password) {
            if(password.length > 72) {
                throw new IllegalArgumentException("Invalid password: max. length is 72 byte");
            }
            return new BCryptOpenBSDProtocol().cryptRaw(1 << cost, salt, password);
        }
    }

    record HashedPassword(String version, int cost, byte[] salt, byte[] hash) {  }
}
