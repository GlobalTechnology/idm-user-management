package org.ccci.idm.user.inspektr.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InspektrSerializationUtils {
    @Nonnull
    public static ToStringHelper userToStringHelper(@Nullable final User user) {
        final ToStringHelper helper = MoreObjects.toStringHelper(User.class);
        if (user != null) {
            helper.add("guid", user.getTheKeyGuid()).add("email", user.getEmail());
        } else {
            helper.addValue(null);
        }
        return helper;
    }

    @Nonnull
    public static String groupToString(@Nonnull final Group group) {
        return group.toString();
    }
}
