package org.ccci.idm.user;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.List;

@Immutable
public final class Group extends Dn {
    private static final long serialVersionUID = 8588784014544957895L;

    private static final String LEGACY_PATH_TYPE = "ou";
    private static final String LEGACY_NAME_TYPE = "cn";
    private static final Function<String, Component> LEGACY_PATH = new Function<String, Component>() {
        @Nullable
        @Override
        public Component apply(@Nullable final String value) {
            return value != null ? new Component(LEGACY_PATH_TYPE, value) : null;
        }
    };

    /**
     * @deprecated Since 0.3.0, use {@link Group#Group(Component...)} instead.
     */
    @Deprecated
    public Group(@Nonnull final String[] path, @Nonnull final String name) {
        super(FluentIterable.of(path).transform(LEGACY_PATH).filter(Predicates.notNull()).append(new Component(LEGACY_NAME_TYPE, name))
                .toList());
    }

    /**
     * @deprecated Since 0.3.0, use {@link Group#Group(Component...)} instead.
     */
    @Deprecated
    public Group(@Nonnull final String... path) {
        this(Arrays.copyOf(path, path.length - 1), path[path.length - 1]);
    }

    public Group(@Nonnull final Component... components) {
        super(components);
        if (getComponents().isEmpty()) {
            throw new IllegalArgumentException("There needs to be at least 1 Component specified");
        }
    }

    public Group(@Nonnull final List<Component> components) {
        super(components);
        if (getComponents().isEmpty()) {
            throw new IllegalArgumentException("There needs to be at least 1 Component specified");
        }
    }

    @Deprecated
    public String[] getPath() {
        final List<Component> components = getComponents();
        return FluentIterable.from(components).limit(components.size() - 1).transform(Component.FUNCTION_VALUE).toArray(String.class);
    }
}
