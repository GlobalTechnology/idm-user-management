package org.ccci.idm.user;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DnTest {
    @Test
    public void verifyEqualsCaseInsensitive() throws Exception {
        final Dn dn1 = new Dn(new Dn.Component("ou", "OuTeR"), new Dn.Component("cn", "inner"));
        final Dn dn2 = new Dn(new Dn.Component("OU", "outer"), new Dn.Component("CN", "INNER"));

        assertThat(dn1, is(dn2));
        assertThat(dn1.hashCode(), is(dn2.hashCode()));
    }
}
