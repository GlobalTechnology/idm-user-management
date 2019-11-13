package org.ccci.idm.user.ldaptive.dao.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.ccci.idm.user.Dn;
import org.ccci.idm.user.LdapGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DnUtilsParameterizedTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{""}, {"ou=groups,ou=idm,dc=cru,dc=org"}});
    }

    private final LdapGroup GROUP;
    private static final String[] PATH = new String[]{"GoogleApps", "Cru", "Cru"};
    private static final String NAME = "Mail";

    private final String groupDn;
    private final String groupSuffix;

    public DnUtilsParameterizedTest(@Nonnull final String baseDn) {
        groupSuffix = baseDn.isEmpty() ? "" : ("," + baseDn);
        groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Dn dn = DnUtils.toDn(baseDn);
        for (final String component : PATH) {
            dn = dn.descendant(new Dn.Component("ou", component));
        }
        GROUP = dn.descendant(new Dn.Component("cn", NAME)).asGroup();
    }

    @Test
    public void verifyDecodeStringValue() throws Exception {
        final LdapGroup group = DnUtils.toDn(groupDn).asGroup();

        assertThat(group, is(this.GROUP));
        assertThat(DnUtils.toString(group), is(equalToIgnoringCase(groupDn)));
        assertThat(group.getDn().getName(), is(equalToIgnoringCase(NAME)));

        assertEquals(groupDn, DnUtils.toString(group));
    }

    @Test
    public void verifyDecodeStringValueCaseInsensitiveDn() throws Exception {
        final LdapGroup group = DnUtils.toDn(groupDn.toUpperCase()).asGroup();

        assertThat(group, is(this.GROUP));
        assertThat(DnUtils.toString(group), is(equalToIgnoringCase(groupDn)));
        assertThat(group.getDn().getName(), is(equalToIgnoringCase(NAME)));

        assertTrue(groupDn.equalsIgnoreCase(DnUtils.toString(group)));
    }

    @Test
    public void verifySourceCasePreserved() throws Exception {
        final String groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;
        assertThat(DnUtils.toString(DnUtils.toDn(groupDn)), is(groupDn));
    }

    @Test
    public void verifyEncodeStringValue() throws Exception {
        final String groupDn = "CN=" + NAME + ",OU=Cru,ou=Cru,OU=GoogleApps" + groupSuffix.toUpperCase();

        assertTrue(groupDn.equalsIgnoreCase(DnUtils.toString(GROUP)));
    }
}
