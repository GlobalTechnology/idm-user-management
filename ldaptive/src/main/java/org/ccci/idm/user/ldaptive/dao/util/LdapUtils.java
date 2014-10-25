package org.ccci.idm.user.ldaptive.dao.util;

import org.ldaptive.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUtils {
    private static final Logger LOG = LoggerFactory.getLogger(LdapUtils.class);

    /**
     * Close the given connection and ignore any thrown exception. This is useful for typical finally blocks in
     * manual Ldap statements.
     *
     * @param conn the Ldap connection to close
     */
    public static void closeConnection(final Connection conn) {
        if (conn != null && conn.isOpen()) {
            try {
                conn.close();
            } catch (final Exception ex) {
                LOG.warn("Could not close ldap connection", ex);
            }
        }
    }
}
