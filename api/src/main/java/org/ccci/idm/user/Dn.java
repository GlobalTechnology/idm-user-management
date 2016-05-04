package org.ccci.idm.user;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Immutable
public class Dn implements Serializable {
    private static final long serialVersionUID = 5510344429904560934L;

    public static final Dn ROOT = new Dn();

    @Nonnull
    private final List<Component> components;

    public Dn(@Nonnull final Component... components) {
        this.components = ImmutableList.copyOf(components);
    }

    public Dn(@Nonnull final List<Component> components) {
        this.components = ImmutableList.copyOf(components);
    }

    @Nonnull
    public final List<Component> getComponents() {
        return components;
    }

    @Nullable
    public final String getName() {
        return components.isEmpty() ? null : components.get(components.size() - 1).value;
    }

    public final boolean isDescendantOf(@Nonnull final Dn ancestor) {
        return ancestor.components.size() < components.size() && isDescendantOfOrEqualTo(ancestor);
    }

    /**
     * @param ancestor the DN we are comparing to this DN.
     * @return true if the DN represented by this object is a descendant of or equal to the specified DN.
     */
    public final boolean isDescendantOfOrEqualTo(@Nonnull final Dn ancestor) {
        return ancestor.components.size() <= components.size() &&
                ancestor.components.equals(components.subList(0, ancestor.components.size()));
    }

    public final boolean isAncestorOf(@Nonnull final Dn descendant) {
        return descendant.isDescendantOf(this);
    }

    /**
     * @param descendant the DN we are comparing to this DN.
     * @return true if the DN represented by this object is an ancestor of or equal to the specified DN.
     */
    public final boolean isAncestorOfOrEqualTo(@Nonnull final Dn descendant) {
        return descendant.isDescendantOfOrEqualTo(this);
    }

    @Nonnull
    public final Dn descendant(@Nonnull final Component... components) {
        return new Dn(ImmutableList.<Component>builder().addAll(this.components).add(components).build());
    }

    @Nonnull
    public final Group asGroup() {
        return new Group(components);
    }

    @Nullable
    public final Dn parent() {
        if (!components.isEmpty()) {
            return new Dn(components.subList(0, components.size() - 1));
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        return o != null && getClass().equals(o.getClass()) && components.equals(((Dn) o).components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("components", components).toString();
    }

    @Immutable
    public static final class Component implements Serializable {
        private static final long serialVersionUID = -5497975422744151635L;

        static final Function<Component, String> FUNCTION_VALUE = new Function<Component, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final Component input) {
                return input != null ? input.value : null;
            }
        };

        @Nonnull
        public final String type;
        @Nonnull
        public final String value;

        public Component(@Nonnull final String type, @Nonnull final String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Component)) { return false; }
            final Component component = (Component) o;
            return type.equalsIgnoreCase(component.type) && value.equalsIgnoreCase(component.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new String[]{type.toLowerCase(Locale.US), value.toLowerCase(Locale.US)});
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("type", type).add("value", value).toString();
        }
    }
}
