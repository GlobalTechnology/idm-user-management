package org.ccci.idm.user.ldaptive.dao.io;

import com.google.common.annotations.VisibleForTesting;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.util.DnUtils;
import org.ldaptive.io.AbstractStringValueTranscoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
public class GroupValueTranscoder extends AbstractStringValueTranscoder<Group> {
    @Nonnull
    private Dn baseDn = Dn.ROOT;

    @Nonnull
    @VisibleForTesting
    String getBaseDnString() {
        return DnUtils.toString(baseDn);
    }

    public void setBaseDn(@Nullable final Dn dn) {
        baseDn = dn != null ? dn : Dn.ROOT;
    }

    /**
     * Set the baseDn as a String. Provided for Spring ease of use.
     *
     * @param dn
     */
    public void setBaseDnString(@Nullable final String dn) {
        setBaseDn(DnUtils.toDn(dn));
    }

    @Override
    public Class<Group> getType() {
        return Group.class;
    }

    @Override
    public String encodeStringValue(@Nonnull final Group group) {
        if (!group.isDescendantOf(baseDn)) {
            throw new IllegalArgumentException("Group \"" + DnUtils.toString(group)
                    + "\" is not a descendant of baseDn \"" + DnUtils.toString(baseDn) + "\"");
        }

        // return generated DN
        return DnUtils.toString(group);
    }

    @Override
    public Group decodeStringValue(@Nonnull final String groupDn) {
        final Dn dn = DnUtils.toDn(groupDn);

        // make sure the group DN ends with the base DN (plus delimiter) if we have a base DN
        if (!dn.isDescendantOf(baseDn)) {
            throw new IllegalGroupDnException(groupDn);
        }

        // return the DN as a Group
        try {
            return dn.asGroup();
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
