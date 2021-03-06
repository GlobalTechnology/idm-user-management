package org.ccci.idm.user.ldaptive;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class GroupTest {
    @Test
    public void verifyEquals() throws Exception {
        final Dn dn = new Dn(new Dn.Component("ou", "group"));
        assertThat(dn.asGroup(), is(not(dn)));
        assertNotEquals(dn, dn.asGroup());
    }
}
