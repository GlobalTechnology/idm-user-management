package org.ccci.idm.user;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Arrays;

@Immutable
public final class Group implements Serializable {
    private static final long serialVersionUID = 8588784014544957895L;

    private final String[] path;

    private final String name;

    public Group(@Nonnull final String[] path, @Nonnull final String name) {
        this.path = path;
        this.name = name;
    }

    public Group(@Nonnull final String... path) {
        if (path.length < 1) {
            throw new IllegalArgumentException("a path for the Group needs to be specified");
        }

        this.path = Arrays.copyOf(path, path.length - 1);
        this.name = path[path.length - 1];
    }

    public String[] getPath() {
        return this.path;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Group)) { return false; }

        final Group other = (Group) o;
        return Arrays.equals(this.path, other.path) && Objects.equal(this.name, other.name);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[]{this.path, this.name});
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("path", this.path).add("name", this.name).toString();
    }
}
