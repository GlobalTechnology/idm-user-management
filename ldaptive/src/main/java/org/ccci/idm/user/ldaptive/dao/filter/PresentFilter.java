package org.ccci.idm.user.ldaptive.dao.filter;

public class PresentFilter extends FieldFilter {
    public PresentFilter(final String field) {
        super(field);
    }

    @Override
    public String format() {
        return "(" + encodeValue(this.field) + "=*)";
    }
}
