package com.github.phoswald.rstm.security.password;

/**
 * A Base64 encoding with a custom alphabet that differs from RFC 4648
 */
class CustomBase64 {

    private static final char[] ENCODE_TABLE = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final byte[] DECODE_TABLE = reverseTable(ENCODE_TABLE);

    private static byte[] reverseTable(char[] encodeTable) {
        byte[] decodeTable = new byte[128];
        for (int i = 0; i < decodeTable.length; i++) {
            decodeTable[i] = -1;
        }
        for (int i = 0; i < encodeTable.length; i++) {
            decodeTable[encodeTable[i]] = (byte) i;
        }
        return decodeTable;
    }

    static int encodedSize(int bytes) {
        return 4 * (bytes / 3) + (bytes % 3 == 0 ? 0 : bytes % 3 + 1);
    }

    static char[] encode(byte[] bytes) {
        int length = encodedSize(bytes.length);
        char[] chars = new char[length];
        int index = 0;
        int end = bytes.length - bytes.length % 3;
        for (int i = 0; i < end; i += 3) {
            chars[index++] = ENCODE_TABLE[(bytes[i] & 0xff) >> 2];
            chars[index++] = ENCODE_TABLE[((bytes[i] & 0x03) << 4) | ((bytes[i + 1] & 0xff) >> 4)];
            chars[index++] = ENCODE_TABLE[((bytes[i + 1] & 0x0f) << 2) | ((bytes[i + 2] & 0xff) >> 6)];
            chars[index++] = ENCODE_TABLE[(bytes[i + 2] & 0x3f)];
        }
        switch (bytes.length % 3) {
            case 1:
                chars[index++] = ENCODE_TABLE[(bytes[end] & 0xff) >> 2];
                chars[index] = ENCODE_TABLE[(bytes[end] & 0x03) << 4];
                break;
            case 2:
                chars[index++] = ENCODE_TABLE[(bytes[end] & 0xff) >> 2];
                chars[index++] = ENCODE_TABLE[((bytes[end] & 0x03) << 4) | ((bytes[end + 1] & 0xff) >> 4)];
                chars[index] = ENCODE_TABLE[((bytes[end + 1] & 0x0f) << 2)];
                break;
        }
        return chars;
    }

    static byte[] decode(char[] chars) {
//        // Ignore trailing '=' padding and whitespace from the input.
//        int charsLimit = chars.length;
//        for (; charsLimit > 0; charsLimit--) {
//            byte c = (byte) chars[charsLimit - 1];
//            if (c != '=' && c != '\n' && c != '\r' && c != ' ' && c != '\t') {
//                break;
//            }
//        }

        // If the input includes whitespace, this output array will be longer than necessary.
        byte[] bytes = new byte[(int) (/* charsLimit */ chars.length * 6L / 8L)];
        int bytesCount = 0;
        int charsCount = 0;

        int word = 0;
        for (int pos = 0; pos < /* charsLimit */ chars.length; pos++) {
            byte c = (byte) chars[pos];

            int bits;
            if (c == '.' || c == '/' || (c >= 'A' && c <= 'z') || (c >= '0' && c <= '9')) {
                bits = DECODE_TABLE[c];
//            } else if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
//                continue;
            } else {
                throw new IllegalArgumentException("Invalid Base64 character: " + c);
            }

            // Append this char's 6 bits to the word.
            word = (word << 6) | (byte) bits;

            // For every 4 chars of input, we accumulate 24 bits of output. Emit 3 bytes.
            charsCount++;
            if (charsCount % 4 == 0) {
                bytes[bytesCount++] = (byte) (word >> 16);
                bytes[bytesCount++] = (byte) (word >> 8);
                bytes[bytesCount++] = (byte) word;
            }
        }

        int lastWordChars = charsCount % 4;
        if (lastWordChars == 1) {
            // We read 1 char followed by "===". But 6 bits is a truncated byte! Fail.
            return new byte[0];
        } else if (lastWordChars == 2) {
            // We read 2 chars followed by "==". Emit 1 byte with 8 of those 12 bits.
            word = word << 12;
            bytes[bytesCount++] = (byte) (word >> 16);
        } else if (lastWordChars == 3) {
            // We read 3 chars, followed by "=". Emit 2 bytes for 16 of those 18 bits.
            word = word << 6;
            bytes[bytesCount++] = (byte) (word >> 16);
            bytes[bytesCount++] = (byte) (word >> 8);
        }

        // If we sized our out array perfectly, we're done.
        if (bytesCount != bytes.length) {
            throw new IllegalStateException();
        }
        return bytes;
    }
}
