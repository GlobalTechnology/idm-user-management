package org.ccci.idm.user.ldaptive.dao.io;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Strings;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder.IllegalGroupDnException;
import org.ccci.idm.user.ldaptive.dao.util.DnUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GroupValueTranscoderTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{null}, {""}, {"ou=groups,ou=idm,dc=cru,dc=org"}});
    }

    private final Group GROUP;
    private static final String[] PATH = new String[]{"GoogleApps", "Cru", "Cru"};
    private static final String NAME = "Mail";

    private final String groupDn;
    private final String groupSuffix;
    @Nonnull
    private final GroupValueTranscoder groupDnResolver;

    public GroupValueTranscoderTest(@Nullable final String baseDn) {
        groupSuffix = Strings.isNullOrEmpty(baseDn) ? "" : ("," + baseDn);
        groupDnResolver = new GroupValueTranscoder();
        groupDnResolver.setBaseDnString(baseDn);
        groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Dn dn = DnUtils.parse(baseDn);
        for (final String component : PATH) {
            dn = dn.descendant(new Dn.Component("ou", component));
        }
        GROUP = dn.descendant(new Dn.Component("cn", NAME)).asGroup();
    }

    @Test
    public void verifyDecodeStringValue() throws Exception {
        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertThat(group, is(this.GROUP));
        assertThat(DnUtils.toString(group), is(equalToIgnoringCase(groupDn)));
        assertThat(group.getName(), is(equalToIgnoringCase(NAME)));

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }

    @Test
    public void verifyDecodeStringValueCaseInsensitiveDn() throws Exception {
        Group group = groupDnResolver.decodeStringValue(groupDn.toUpperCase());

        assertThat(group, is(this.GROUP));
        assertThat(DnUtils.toString(group), is(equalToIgnoringCase(groupDn)));
        assertThat(group.getName(), is(equalToIgnoringCase(NAME)));

        assertTrue(groupDn.equalsIgnoreCase(groupDnResolver.encodeStringValue(group)));
    }

    @Test
    public void verifySourceCasePreserved() throws Exception {
        final String groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;
        assertThat(groupDnResolver.encodeStringValue(groupDnResolver.decodeStringValue(groupDn)), is(groupDn));
    }

    @Test
    public void verifyEncodeStringValue() throws Exception {
        final String groupDn = "CN=" + NAME + ",OU=Cru,ou=Cru,OU=GoogleApps" + groupSuffix.toUpperCase();

        assertTrue(groupDn.equalsIgnoreCase(groupDnResolver.encodeStringValue(GROUP)));
    }

    @Test
    public void testEdgeCases() throws Exception {
        // test groupDn == baseDn
        try {
            groupDnResolver.decodeStringValue(groupDnResolver.getBaseDnString());
            fail("parsing the baseDn should have triggered an IllegalArgumentException");
        } catch (final IllegalGroupDnException expected) {
        }
    }
}
