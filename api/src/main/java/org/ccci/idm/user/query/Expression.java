package org.ccci.idm.user.query;

import javax.annotation.Nonnull;
import java.io.Serializable;

public interface Expression extends Serializable {
    default Expression and(Expression... expressions) {
        return new BooleanExpression(BooleanExpression.Type.AND, this, expressions);
    }

    default Expression or(Expression... expressions) {
        return new BooleanExpression(BooleanExpression.Type.OR, this, expressions);
    }

    default Expression not() {
        return new NotExpression(this);
    }

    static Expression not(@Nonnull final Expression expression) {
        return expression.not();
    }
}
