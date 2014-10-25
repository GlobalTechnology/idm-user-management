package org.ccci.idm.user.ldaptive.dao.filter;

import org.ldaptive.SearchFilter;

public class NotFilter extends BooleanFilter {
    public NotFilter(final SearchFilter filter) {
        super("!", filter);
    }
}
