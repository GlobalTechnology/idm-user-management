package org.ccci.idm.user.ldaptive.dao.filter;

import com.google.common.collect.ObjectArrays;
import org.ldaptive.SearchFilter;

import java.util.Objects;

public abstract class BaseFilter extends SearchFilter {
    public AndFilter and(final SearchFilter... filters) {
        return new AndFilter(ObjectArrays.concat(this, filters));
    }

    public OrFilter or(final SearchFilter... filters) {
        return new OrFilter(ObjectArrays.concat(this, filters));
    }

    public NotFilter not() {
        return new NotFilter(this);
    }

    @Override
    public boolean equals(final Object o) {
        // XXX: we ignore underlying equals because we don't care about the super object
        return this == o || (o != null && Objects.equals(this.getClass(), o.getClass()));
    }

    @Override
    public int hashCode() {
        //XXX: we ignore the underlying hashCode because we don't care about the super object
        return 1;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.format();
    }
}
