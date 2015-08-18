package org.ccci.idm.user.util;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class encapsulating a particular hashing algorithm.
 *
 * author@lee.braddock
 */
public class HashUtility {
    private static final Logger LOG = LoggerFactory.getLogger(HashUtility.class);

    private static StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    public static String getHash(final String string) {
        return passwordEncryptor.encryptPassword(string);
    }

    public static boolean checkHash(final String string, final String hash) {
        try {
            return passwordEncryptor.checkPassword(string, hash);
        } catch (final Exception e) {
            // log exception, only include hash at high log levels
            LOG.error("error checking value against hash '{}'", (LOG.isDebugEnabled() ? hash : "**hidden**"), e);

            // fail if there was an exception
            return false;
        }
    }
}
