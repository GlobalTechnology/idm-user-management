package org.ccci.idm.user.spring.ldap.dao.util;

import org.springframework.ldap.core.LdapEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class LdapUtils {
    /**
     * Utility method to replace the placeholders in the filter with the proper values from the userName.
     *
     * @param filter
     * @param userName
     * @return the filtered string populated with the username
     */
    public static String getFilterWithValues(final String filter, final String userName) {
        final Map<String, String> properties = new HashMap<String, String>();
        final String[] userDomain = userName.split("@");

        properties.put("%u", userName);
        properties.put("%U", userDomain[0]);

        if (userDomain.length > 1) {
            properties.put("%d", userDomain[1]);

            final String[] dcArray = userDomain[1].split("\\.");
            for (int i = 0; i < dcArray.length; i++) {
                properties.put("%" + (i + 1), dcArray[dcArray.length - 1 - i]);
            }
        }

        // generate & return filter
        String newFilter = filter;
        for (final String key : properties.keySet()) {
            final String value = LdapEncoder.nameEncode(properties.get(key));
            newFilter = newFilter.replaceAll(key, Matcher.quoteReplacement(value));
        }
        return newFilter;
    }
}
