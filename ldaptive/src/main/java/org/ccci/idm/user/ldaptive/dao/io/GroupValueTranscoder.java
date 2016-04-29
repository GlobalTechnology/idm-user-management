package org.ccci.idm.user.ldaptive.dao.io;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ldaptive.DnParser;
import org.ldaptive.LdapAttribute;
import org.ldaptive.io.AbstractStringValueTranscoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class GroupValueTranscoder extends AbstractStringValueTranscoder<Group> {
    private static final String delimiter = ",";
    private static final String valueDelimiter = "=";

    private String baseDn = "";

    public String getBaseDn() {
        return this.baseDn;
    }

    public void setBaseDn(@Nullable final String dn) {
        this.baseDn = Strings.nullToEmpty(dn);
    }

    @Deprecated
    public String getPathRdnAttr() {
        return "ou";
    }

    @Deprecated
    public void setPathRdnAttr(@Nullable final String rdnAttr) {}

    @Deprecated
    public String getNameRdnAttr() {
        return "cn";
    }

    @Deprecated
    public void setNameRdnAttr(@Nonnull final String rdnAttr) {}

    @Override
    public Class<Group> getType() {
        return Group.class;
    }

    @Override
    public String encodeStringValue(@Nonnull final Group group) {
        final StringBuilder sb = new StringBuilder();

        // append components
        for (final Dn.Component component : Lists.reverse(group.getComponents())) {
            if(sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(component.type).append(valueDelimiter).append(LdapAttribute.escapeValue(component.value));
        }

        if (baseDn.length() > 0) {
            sb.append(delimiter).append(this.baseDn);
        }

        // return generated DN
        return sb.toString();
    }

    @Override
    public Group decodeStringValue(@Nonnull final String groupDn) {
        // make sure the group DN ends with the base DN (plus delimiter) if we have a base DN
        if (baseDn.length() > 0 && !groupDn.toLowerCase().endsWith(delimiter + baseDn.toLowerCase())) {
            throw new IllegalGroupDnException(groupDn);
        }

        final String relative;
        if (baseDn.length() > 0) {
            relative = groupDn.substring(0, groupDn.length() - baseDn.length() - 1);
        } else {
            relative = groupDn;
        }

        final ImmutableList.Builder<Dn.Component> builder = ImmutableList.builder();
        for (final LdapAttribute attribute : Lists.reverse(DnParser.convertDnToAttributes(relative))) {
            builder.add(new Dn.Component(attribute.getName(), attribute.getStringValue()));
        }
        final List<Dn.Component> components = builder.build();
        if (components.isEmpty()) {
            throw new IllegalGroupDnException(groupDn);
        }
        return new Group(components);
    }

    public static class IllegalGroupDnException extends IllegalArgumentException {
        private static final long serialVersionUID = 3119012756644385809L;

        @Nonnull
        private final String groupDn;

        public IllegalGroupDnException(@Nonnull final String groupDn) {
            super("Group " + groupDn + " cannot be parsed");
            this.groupDn = groupDn;
        }

        @Nonnull
        public String getGroupDn() {
            return groupDn;
        }
    }
}
