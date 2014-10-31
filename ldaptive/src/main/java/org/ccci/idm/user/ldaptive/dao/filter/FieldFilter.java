package org.ccci.idm.user.ldaptive.dao.filter;

import com.google.common.base.Objects;

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
        return super.equals(o) && Objects.equal(this.field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.field);
    }
}
