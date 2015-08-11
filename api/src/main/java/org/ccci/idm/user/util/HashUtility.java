package org.ccci.idm.user.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Utility class encapsulating a particular hashing algorithm.
 *
 * author@lee.braddock
 */
public class HashUtility {

    private static final Integer BCRYPT_WORK_FACTOR = 12;

    public static String getHash(final String string) {
        return BCrypt.hashpw(string, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    public static boolean checkHash(final String string, final String hash) {
        return BCrypt.checkpw(string, hash);
    }
}