package org.ccci.idm.user;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class RelativeDn extends Dn<RelativeDn> {
    private static final long serialVersionUID = -3391132126454968389L;

    public RelativeDn(@Nonnull final Component... components) {
        super(components);
    }

    public RelativeDn(@Nonnull final List<Component> components) {
        super(components);
    }

    @Override
    public final boolean isRelative() {
        return true;
    }

    @Nonnull
    @Override
    public final RelativeDn descendant(@Nonnull final Component... components) {
        return new RelativeDn(ImmutableList.<Component>builder().addAll(this.components).add(components).build());
    }

    @Nullable
    @Override
    public RelativeDn parent() {
        if (!components.isEmpty()) {
            return new RelativeDn(components.subList(0, components.size() - 1));
        }
        return null;
    }

    @Nonnull
    public final AbsoluteDn resolve(@Nonnull final AbsoluteDn baseDn) {
        return new AbsoluteDn(ImmutableList.<Component>builder()
                .addAll(baseDn.getComponents())
                .addAll(getComponents())
                .build());
    }
}
