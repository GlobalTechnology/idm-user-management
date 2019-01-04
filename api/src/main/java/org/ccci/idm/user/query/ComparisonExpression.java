package org.ccci.idm.user.query;

import org.ccci.idm.user.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ComparisonExpression implements Expression {
    public enum Type {EQ, LIKE}

    @Nonnull
    private final Type type;
    @Nonnull
    private final Attribute attribute;
    @Nullable
    private final String value;
    @Nullable
    private final Group group;


    ComparisonExpression(@Nonnull final Type type, @Nonnull final Attribute attribute, @Nonnull final String value) {
        this.type = type;
        this.attribute = attribute;
        this.value = value;
        this.group = null;
    }

    ComparisonExpression(@Nonnull final Type type, @Nonnull final Attribute attribute, @Nonnull final Group group) {
        this.type = type;
        this.attribute = attribute;
        this.value = null;
        this.group = group;
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    @Nonnull
    public Attribute getAttribute() {
        return attribute;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    @Nullable
    public Group getGroup() {
        return group;
    }
}
