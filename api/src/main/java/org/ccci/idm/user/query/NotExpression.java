package org.ccci.idm.user.query;

import javax.annotation.Nonnull;

public class NotExpression implements Expression {
    private static final long serialVersionUID = -7321113440722414788L;

    @Nonnull
    private final Expression component;

    NotExpression(@Nonnull final Expression expression) {
        component = expression;
    }

    @Nonnull
    public Expression getComponent() {
        return component;
    }

    @Override
    public Expression not() {
        return component;
    }
}
