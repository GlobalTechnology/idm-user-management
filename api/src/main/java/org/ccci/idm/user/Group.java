package org.ccci.idm.user;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public final class Group extends AbsoluteDn {
    private static final long serialVersionUID = 8588784014544957895L;

    public Group(@Nonnull final List<Component> components) {
        super(components);
        if (getComponents().isEmpty()) {
            throw new IllegalArgumentException("There needs to be at least 1 Component specified");
        }
    }
}
