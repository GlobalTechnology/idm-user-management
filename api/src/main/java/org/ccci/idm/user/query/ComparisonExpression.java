package org.ccci.idm.user.query;

import com.google.common.base.Strings;
import kotlin.text.StringsKt;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ComparisonExpression implements Expression {
    private static final long serialVersionUID = 6470937370962349745L;

    public enum Type {EQ, LIKE, SW}

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

    @Override
    public boolean matches(@Nonnull final User user) {
        switch (attribute) {
            case GUID:
                return matches(user.getTheKeyGuid());
            case EMAIL:
                return matches(user.getEmail());
            case EMAIL_ALIAS:
                return user.getCruProxyAddresses().stream().anyMatch(this::matches);
            case FIRST_NAME:
                return matches(user.getFirstName());
            case LAST_NAME:
                return matches(user.getLastName());
            case US_EMPLOYEE_ID:
                return matches(user.getEmployeeId());
            case US_DESIGNATION:
                return matches(user.getCruDesignation());
            case GROUP:
                return user.getGroups().stream().anyMatch(g -> g.equals(group));
            default:
                return false;
        }
    }

    private boolean matches(@Nullable final String value) {
        switch (type) {
            case EQ:
                return StringsKt.equals(value, this.value, true);
            case SW:
                return StringsKt.startsWith(Strings.nullToEmpty(value), Strings.nullToEmpty(this.value), true);
            case LIKE:
                throw new UnsupportedOperationException("LIKE comparisons are not currently supported");
        }

        return false;
    }
}
