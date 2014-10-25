package org.ccci.idm.user.ldaptive.dao.filter;

import java.util.Objects;

public abstract class FieldFilter extends BaseFilter {
    protected final String field;

    protected FieldFilter(final String field) {
        this.field = field != null ? field : "";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof FieldFilter)) { return false; }

        final FieldFilter that = (FieldFilter) o;
        return super.equals(o) && Objects.equals(this.field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.field);
    }
}
