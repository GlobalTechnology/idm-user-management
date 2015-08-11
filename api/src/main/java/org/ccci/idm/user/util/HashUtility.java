package org.ccci.idm.user.util;

import org.jasypt.util.password.StrongPasswordEncryptor;

/**
 * Utility class encapsulating a particular hashing algorithm.
 *
 * author@lee.braddock
 */
public class HashUtility {

    private static StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    public static String getHash(final String string) {
        return passwordEncryptor.encryptPassword(string);
    }

    public static boolean checkHash(final String string, final String hash) {
        return passwordEncryptor.checkPassword(string, hash);
    }
}
