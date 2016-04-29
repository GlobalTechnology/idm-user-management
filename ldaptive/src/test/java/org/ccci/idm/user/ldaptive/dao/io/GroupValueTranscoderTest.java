package org.ccci.idm.user.ldaptive.dao.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Strings;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder.IllegalGroupDnException;
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

    private final String groupSuffix;
    @Nonnull
    private final GroupValueTranscoder groupDnResolver;

    public GroupValueTranscoderTest(@Nullable final String baseDn) {
        groupSuffix = Strings.isNullOrEmpty(baseDn) ? "" : ("," + baseDn);
        groupDnResolver = new GroupValueTranscoder();
        groupDnResolver.setBaseDn(baseDn);
    }

    private String name = "Mail";
    private String[] path = new String[] {"GoogleApps", "Cru", "Cru"};

    @Test
    public void testGroupDnResolver() throws Exception {
        final String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }

    @Test
    public void testGroupDnResolverCaseInsensitiveDn() throws Exception {
        final String groupDn = "CN=" + name + ",OU=Cru,ou=Cru,OU=GoogleApps" + groupSuffix.toUpperCase();

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));

        assertTrue(groupDn.equalsIgnoreCase(groupDnResolver.encodeStringValue(group)));
    }

    @Test
    public void testGroupNotMutated() throws Exception {
        final String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps" + groupSuffix;

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }

    @Test
    public void testEdgeCases() throws Exception {
        // test groupDn == baseDn
        try {
            groupDnResolver.decodeStringValue(groupDnResolver.getBaseDn());
            fail("parsing the baseDn should have triggered an IllegalArgumentException");
        } catch (final IllegalGroupDnException expected) {
        }
    }
}
