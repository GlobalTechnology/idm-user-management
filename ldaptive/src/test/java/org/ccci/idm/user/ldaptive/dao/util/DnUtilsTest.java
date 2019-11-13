package org.ccci.idm.user.ldaptive.dao.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ccci.idm.user.ldaptive.Dn;
import org.junit.Test;

public class DnUtilsTest {
    @Test
    public void verifyParseRoot() throws Exception {
        assertThat(DnUtils.toDn(null), is(Dn.ROOT));
        assertThat(DnUtils.toDn(""), is(Dn.ROOT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyParseInvalidDn() throws Exception {
        DnUtils.toDn("dcorg");
    }

    @Test
    public void verifyParse() throws Exception {
        final Dn expected = Dn.ROOT.descendant(new Dn.Component("dc", "org"), new Dn.Component("ou", "groups"), new
                Dn.Component("cn", "name"));
        final String rawDn = "cn=nAmE,OU=groups,dC=org";

        assertThat(DnUtils.toDn(rawDn), is(expected));
        assertThat(DnUtils.toDn(rawDn.toUpperCase()), is(expected));
        assertThat(DnUtils.toDn(rawDn.toLowerCase()), is(expected));
    }
}
