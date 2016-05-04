package org.ccci.idm.user.ldaptive.dao.io;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Strings;
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

    private static String NAME = "Mail";
    private static String[] PATH = new String[]{"GoogleApps", "Cru", "Cru"};
    private Group GROUP = new Group(PATH, NAME);

    private final String groupSuffix;
    @Nonnull
    private final GroupValueTranscoder groupDnResolver;

    public GroupValueTranscoderTest(@Nullable final String baseDn) {
        groupSuffix = Strings.isNullOrEmpty(baseDn) ? "" : ("," + baseDn);
        groupDnResolver = new GroupValueTranscoder();
        groupDnResolver.setBaseDn(baseDn);
    }

    @Test
    public void verifyDecodeStringValue() throws Exception {
        final String groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(NAME, group.getName());
        assertThat(group, is(GROUP));
        assertEquals(Arrays.toString(PATH), Arrays.toString(group.getPath()));

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }

    @Test
    public void verifyDecodeStringValueCaseInsensitiveDn() throws Exception {
        final String groupDn = "CN=" + NAME + ",OU=Cru,ou=Cru,OU=GoogleApps" + groupSuffix.toUpperCase();

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertThat(group, is(GROUP));
        assertEquals(NAME, group.getName());
        assertEquals(Arrays.toString(PATH), Arrays.toString(group.getPath()));

        assertTrue(groupDn.equalsIgnoreCase(groupDnResolver.encodeStringValue(group)));
    }

    @Test
    public void testGroupNotMutated() throws Exception {
        final String groupDn = "cn=" + NAME + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertThat(group, is(GROUP));
        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
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
            groupDnResolver.decodeStringValue(DnUtils.toString(groupDnResolver.getBaseDn()));
            fail("parsing the baseDn should have triggered an IllegalArgumentException");
        } catch (final IllegalGroupDnException expected) {
        }
    }
}
