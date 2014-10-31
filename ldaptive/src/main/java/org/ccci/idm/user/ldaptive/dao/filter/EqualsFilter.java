package org.ccci.idm.user.ldaptive.dao.filter;

import com.google.common.base.Objects;

public class EqualsFilter extends FieldFilter {
    private final String value;

    public EqualsFilter(final String field, final String value) {
        super(field);
        this.value = value != null ? value : "";
    }

    @Override
    public String format() {
        return "(" + encodeValue(this.field) + "=" + encodeValue(this.value) + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof EqualsFilter)) { return false; }

        final EqualsFilter that = (EqualsFilter) o;
        return super.equals(o) && Objects.equal(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.value);
    }
}
