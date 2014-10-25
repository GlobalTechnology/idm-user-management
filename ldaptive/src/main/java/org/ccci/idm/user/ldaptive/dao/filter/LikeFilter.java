package org.ccci.idm.user.ldaptive.dao.filter;

import java.util.Objects;

public class LikeFilter extends FieldFilter {
    private final String pattern;

    public LikeFilter(final String field, final String pattern) {
        super(field);
        this.pattern = pattern != null ? pattern : "";
    }

    @Override
    public String format() {
        return "(" + encodeValue(this.field) + "=" + encodeLikeValue(this.pattern) + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof LikeFilter)) { return false; }

        final LikeFilter that = (LikeFilter) o;
        return super.equals(o) && Objects.equals(this.pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.pattern);
    }

    private static String encodeLikeValue(final String s) {
        final int len = s.length();
        final StringBuilder sb = new StringBuilder(len);
        char ch;
        for (int i = 0; i < len; i++) {
            ch = s.charAt(i);
            switch (ch) {
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\\':
                    sb.append("\\5c");
                    break;
                case 0:
                    sb.append("\\00");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
