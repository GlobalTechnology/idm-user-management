package org.ccci.idm.user.ldaptive.dao.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ccci.idm.user.AbsoluteDn;
import org.ccci.idm.user.Dn;
import org.ldaptive.DnParser;
import org.ldaptive.LdapAttribute;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DnUtils {
    private static final String DELIMITER = ",";
    private static final String VALUE_DELIMITER = "=";

    /**
     * Parse a DN string into a {@link Dn object}.
     *
     * @param rawDn the raw Dn
     * @return the parsed Dn
     * @throws IllegalArgumentException when the rawDn isn't a well formed DN
     */
    @Nonnull
    public static AbsoluteDn toDn(@Nullable final String rawDn) {
        if (rawDn == null) {
            return AbsoluteDn.ROOT;
        }

        final ImmutableList.Builder<Dn.Component> builder = ImmutableList.builder();
        for (final LdapAttribute attribute : Lists.reverse(DnParser.convertDnToAttributes(rawDn))) {
            builder.add(new Dn.Component(attribute.getName(), attribute.getStringValue()));
        }
        return new AbsoluteDn(builder.build());
    }

    /**
     * Parse a DN string into a {@link Dn object}. If the DN is not well formed this method returns null instead of
     * throwing an {@link IllegalArgumentException}
     *
     * @param rawDn the DN to parse into a {@link Dn} object
     * @return the parsed {@link Dn} or null if the rawDn was not well-formed
     */
    @Nullable
    public static AbsoluteDn toDnSafe(@Nullable final String rawDn) {
        try {
            return toDn(rawDn);
        } catch (final Exception e) {
            return null;
        }
    }

    @Nonnull
    public static String toString(@Nonnull final Dn<?> dn) {
        final StringBuilder sb = new StringBuilder();

        // append components
        for (final Dn.Component component : Lists.reverse(dn.getComponents())) {
            if (sb.length() > 0) {
                sb.append(DELIMITER);
            }
            sb.append(component.type).append(VALUE_DELIMITER).append(LdapAttribute.escapeValue(component.value));
        }

        // return generated DN
        return sb.toString();
    }
}
