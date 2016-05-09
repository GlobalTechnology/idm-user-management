package org.ccci.idm.user.ldaptive.dao.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ccci.idm.user.AbsoluteDn;
import org.ccci.idm.user.Dn;
import org.junit.Test;

public class DnUtilsTest {
    @Test
    public void verifyParseRoot() throws Exception {
        assertThat(DnUtils.toDn(null), is(AbsoluteDn.ROOT));
        assertThat(DnUtils.toDn(""), is(AbsoluteDn.ROOT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyParseInvalidDn() throws Exception {
        DnUtils.toDn("dcorg");
    }

    @Test
    public void verifyParse() throws Exception {
        final Dn expected = AbsoluteDn.ROOT.child("dc", "org").child("ou", "groups").child("cn", "name");
        final String rawDn = "cn=nAmE,OU=groups,dC=org";

        assertThat(DnUtils.toDn(rawDn), is(expected));
        assertThat(DnUtils.toDn(rawDn.toUpperCase()), is(expected));
        assertThat(DnUtils.toDn(rawDn.toLowerCase()), is(expected));
    }
}
