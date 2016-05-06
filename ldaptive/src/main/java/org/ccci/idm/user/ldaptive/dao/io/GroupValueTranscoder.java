package org.ccci.idm.user.ldaptive.dao.io;

import com.google.common.annotations.VisibleForTesting;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.util.DnUtils;
import org.ldaptive.io.AbstractStringValueTranscoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GroupValueTranscoder extends AbstractStringValueTranscoder<Group> {
    private static final String delimiter = ",";

    @Nonnull
    private Dn baseDn = Dn.ROOT;

    /**
     * @deprecated Since 0.3.0, kept for old spring config compatibility.
     */
    @Nonnull
    @Deprecated
    public String getBaseDn() {
        return getBaseDnString();
    }

    @Nonnull
    @VisibleForTesting
    String getBaseDnString() {
        return DnUtils.toString(baseDn);
    }

    public void setBaseDn(@Nullable final Dn dn) {
        baseDn = dn != null ? dn : Dn.ROOT;
    }

    /**
     * @deprecated Since v0.3.0, use {@link GroupValueTranscoder#setBaseDnString(String)} instead.
     */
    @Deprecated
    public void setBaseDn(@Nullable final String dn) {
        setBaseDnString(dn);
    }

    /**
     * Set the baseDn as a String. Provided for Spring ease of use.
     *
     * @param dn
     */
    public void setBaseDnString(@Nullable final String dn) {
        setBaseDn(DnUtils.parse(dn));
    }

    /**
     * @deprecated Since v0.3.0, this is no longer required for {@link GroupValueTranscoder} to work.
     */
    @Deprecated
    public String getPathRdnAttr() {
        return "ou";
    }

    /**
     * @deprecated Since v0.3.0, this is no longer required for {@link GroupValueTranscoder} to work.
     */
    @Deprecated
    public void setPathRdnAttr(@Nullable final String rdnAttr) {}

    /**
     * @deprecated Since v0.3.0, this is no longer required for {@link GroupValueTranscoder} to work.
     */
    @Deprecated
    public String getNameRdnAttr() {
        return "cn";
    }

    /**
     * @deprecated Since v0.3.0, this is no longer required for {@link GroupValueTranscoder} to work.
     */
    @Deprecated
    public void setNameRdnAttr(@Nonnull final String rdnAttr) {}

    @Override
    public Class<Group> getType() {
        return Group.class;
    }

    @Override
    public String encodeStringValue(@Nonnull final Group group) {
        final StringBuilder sb = new StringBuilder(DnUtils.toString(group));

        if (baseDn.getComponents().size() > 0) {
            sb.append(delimiter).append(DnUtils.toString(baseDn));
        }

        // return generated DN
        return sb.toString();
    }

    @Override
    public Group decodeStringValue(@Nonnull final String groupDn) {
        final Dn dn = DnUtils.parse(groupDn);

        // make sure the group DN ends with the base DN (plus delimiter) if we have a base DN
        if (!dn.isDescendantOf(baseDn)) {
            throw new IllegalGroupDnException(groupDn);
        }

        // return the relative DN
        try {
            return new Dn(dn.getComponents().subList(baseDn.getComponents().size(), dn.getComponents().size()))
                    .asGroup();
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
