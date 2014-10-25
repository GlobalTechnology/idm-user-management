package org.ccci.idm.user.ldaptive.dao.filter;

import org.ldaptive.SearchFilter;

import java.util.Arrays;
import java.util.Objects;

public abstract class BooleanFilter extends BaseFilter {
    protected final String type;

    protected final SearchFilter[] filters;

    protected BooleanFilter(final String type, final SearchFilter... filters) {
        this.type = type != null ? type : "";
        this.filters = filters != null ? filters : new SearchFilter[0];
    }

    @Override
    public String format() {
        final StringBuilder sb = new StringBuilder((filters.length * 32) + 3);
        sb.append("(");
        sb.append(this.type);
        for (final SearchFilter filter : filters) {
            sb.append(filter.format());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof BooleanFilter)) { return false; }

        final BooleanFilter that = (BooleanFilter) o;
        return super.equals(o) && Objects.equals(this.type, that.type) && Arrays.equals(this.filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[]{super.hashCode(), this.type, this.filters});
    }
}
