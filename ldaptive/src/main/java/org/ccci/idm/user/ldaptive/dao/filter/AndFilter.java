package org.ccci.idm.user.ldaptive.dao.filter;

import com.google.common.collect.ObjectArrays;
import org.ldaptive.SearchFilter;

public final class AndFilter extends BooleanFilter {
    public AndFilter(final SearchFilter... filters) {
        super("&", filters);
    }

    @Override
    public AndFilter and(final SearchFilter... filters) {
        return new AndFilter(ObjectArrays.concat(this.filters, filters, SearchFilter.class));
    }
}
