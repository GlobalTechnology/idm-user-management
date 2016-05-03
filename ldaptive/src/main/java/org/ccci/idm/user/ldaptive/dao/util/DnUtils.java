package org.ccci.idm.user.ldaptive.dao.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ccci.idm.user.Dn;
import org.ldaptive.DnParser;
import org.ldaptive.LdapAttribute;

import javax.annotation.Nonnull;

public class DnUtils {
    private static final String DELIMITER = ",";
    private static final String VALUE_DELIMITER = "=";

    @Nonnull
    public static Dn parse(@Nonnull final String rdn) {
        final ImmutableList.Builder<Dn.Component> builder = ImmutableList.builder();
        for (final LdapAttribute attribute : Lists.reverse(DnParser.convertDnToAttributes(rdn))) {
            builder.add(new Dn.Component(attribute.getName(), attribute.getStringValue()));
        }
        return new Dn(builder.build());
    }

    @Nonnull
    public static String toString(@Nonnull final Dn dn) {
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
