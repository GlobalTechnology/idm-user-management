package org.ccci.idm.user;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class AbsoluteDn extends Dn<AbsoluteDn> {
    private static final long serialVersionUID = 1295192049546262920L;

    public static final AbsoluteDn ROOT = new AbsoluteDn();

    public AbsoluteDn(@Nonnull final Component... components) {
        super(components);
    }

    public AbsoluteDn(@Nonnull final List<Component> components) {
        super(components);
    }

    @Override
    public final boolean isRelative() {
        return false;
    }

    @Nullable
    public final AbsoluteDn parent() {
        if (!components.isEmpty()) {
            return new AbsoluteDn(components.subList(0, components.size() - 1));
        }
        return null;
    }

    @Nonnull
    public final AbsoluteDn descendant(@Nonnull final Component... components) {
        return new AbsoluteDn(ImmutableList.<Component>builder().addAll(this.components).add(components).build());
    }

    @Nonnull
    public final Group asGroup() {
        return new Group(components);
    }
}
