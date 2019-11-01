package org.ccci.idm.user.inspektr.util;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.FluentIterable;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class InspektrSerializationUtils {
    private static final Joiner COMPONENT_JOINER = Joiner.on(",");
    private static final Function<Dn.Component, String> COMPONENT_SERIALIZER = new Function<Dn.Component, String>() {
        @Nullable
        @Override
        public String apply(@Nullable final Dn.Component input) {
            return input != null ? input.type + "=" + input.value : null;
        }
    };

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
        final List<String> components = FluentIterable.from(group.getComponents())
                .transform(COMPONENT_SERIALIZER)
                .toList()
                .reverse();
        return MoreObjects.toStringHelper(group)
                .addValue(COMPONENT_JOINER.join(components))
                .toString();
    }
}
