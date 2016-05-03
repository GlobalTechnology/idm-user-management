package org.ccci.idm.user.ldaptive.dao.io;

import com.google.common.base.Strings;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.util.DnUtils;
import org.ldaptive.io.AbstractStringValueTranscoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        final StringBuilder sb = new StringBuilder(DnUtils.toString(group));

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

        try {
            return DnUtils.parse(relative).asGroup();
        } catch (final IllegalArgumentException e) {
            throw new IllegalGroupDnException(groupDn);
        }
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
