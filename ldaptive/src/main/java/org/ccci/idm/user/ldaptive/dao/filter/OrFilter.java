package org.ccci.idm.user.ldaptive.dao.filter;

import com.google.common.collect.ObjectArrays;
import org.ldaptive.SearchFilter;

public final class OrFilter extends BooleanFilter {
    public OrFilter(final SearchFilter... filters) {
        super("|", filters);
    }

    @Override
    public OrFilter or(final SearchFilter... filters) {
        return new OrFilter(ObjectArrays.concat(this.filters, filters, SearchFilter.class));
    }
}
