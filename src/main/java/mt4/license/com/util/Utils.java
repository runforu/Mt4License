package mt4.license.com.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Utils {
    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String getSalt() {
        SecureRandom sr = null;
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
        }
        if (sr != null) {
            byte[] salt = new byte[8];
            sr.nextBytes(salt);
            return byteArrayToHexString(salt);
        }
        return null;
    }
}
