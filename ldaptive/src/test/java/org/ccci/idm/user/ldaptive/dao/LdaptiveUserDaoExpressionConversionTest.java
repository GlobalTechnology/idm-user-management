package org.ccci.idm.user.ldaptive.dao;

import static org.ccci.idm.user.query.Attribute.FIRST_NAME;
import static org.ccci.idm.user.query.Attribute.GUID;
import static org.ccci.idm.user.query.Attribute.LAST_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.ccci.idm.user.User;
import org.ccci.idm.user.query.Expression;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;

@RunWith(JUnitParamsRunner.class)
public class LdaptiveUserDaoExpressionConversionTest {
    private static class UnsupportedExpression implements Expression {
        @Override
        public boolean matches(@Nonnull final User user) {
            return false;
        }
    }

    private LdaptiveUserDao dao = new LdaptiveUserDao();

    @Test
    public void verifyNullExpression() {
        assertNull(dao.convertExpressionToFilter(null));
    }

    @Test
    public void verifyUnsupportedExpression() {
        try {
            dao.convertExpressionToFilter(new UnsupportedExpression());
            fail();
        } catch (final IllegalArgumentException expected) {
            assertThat(expected, hasProperty("message", containsString("Unsupported")));
        }
    }

    @Test
    @Parameters(method = "comparisonExpressions, compoundExpressions, escapedExpressions")
    public void verifyConversion(final Expression expression, final String filter) {
        assertEquals(filter, dao.convertExpressionToFilter(expression).format());
    }

    private Object comparisonExpressions() {
        return new Object[] {
                new Object[] {FIRST_NAME.eq("First"), "(givenName=First)"},
                new Object[] {LAST_NAME.like("L*"), "(sn=L*)"},
                new Object[] {GUID.eq("01234567-89AB-CDEF-0123-456789ABCDEF"), "(|(thekeyGuid=01234567-89AB-CDEF-0123-456789ABCDEF)(&(ccciGuid=01234567-89AB-CDEF-0123-456789ABCDEF)(!(thekeyGuid=*))))"}
        };
    }

    private Object compoundExpressions() {
        return new Object[] {
                new Object[] {FIRST_NAME.eq("F").and(LAST_NAME.eq("L")), "(&(givenName=F)(sn=L))"},
                new Object[] {FIRST_NAME.eq("F").or(LAST_NAME.eq("L")), "(|(givenName=F)(sn=L))"},
                new Object[] {FIRST_NAME.eq("F").not(), "(!(givenName=F))"},
        };
    }

    private Object escapedExpressions() {
        return new Object[] {
                new Object[] {LAST_NAME.eq("*()"), "(sn=\\2a\\28\\29)"},
                new Object[] {LAST_NAME.like("*()"), "(sn=*\\28\\29)"},
        };
    }
}
