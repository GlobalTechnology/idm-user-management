package org.ccci.idm.user.ldaptive.dao.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ccci.idm.user.Dn;
import org.junit.Test;

public class DnUtilsTest {
    @Test
    public void verifyParseRoot() throws Exception {
        assertThat(DnUtils.parse(null), is(Dn.ROOT));
        assertThat(DnUtils.parse(""), is(Dn.ROOT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyParseInvalidDn() throws Exception {
        DnUtils.parse("dcorg");
    }

    @Test
    public void verifyParse() throws Exception {
        final Dn expected = Dn.ROOT.descendant(new Dn.Component("dc", "org"), new Dn.Component("ou", "groups"), new
                Dn.Component("cn", "name"));
        final String rawDn = "cn=nAmE,OU=groups,dC=org";

        assertThat(DnUtils.parse(rawDn), is(expected));
        assertThat(DnUtils.parse(rawDn.toUpperCase()), is(expected));
        assertThat(DnUtils.parse(rawDn.toLowerCase()), is(expected));
    }
}
