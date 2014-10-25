package org.ccci.idm.user.ldaptive.dao.filter;

public class PresenceFilter extends FieldFilter {
    public PresenceFilter(final String field) {
        super(field);
    }

    @Override
    public String format() {
        return "(" + encodeValue(this.field) + "=*)";
    }
}
