package org.ccci.idm.user.ldaptive.dao.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import org.ccci.idm.user.Group;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

public class GroupValueTranscoderTest
{
    private static GroupValueTranscoder groupDnResolver;
    private static final String baseDn = "ou=groups,ou=idm,dc=cru,dc=org";

    private void assumeConfigured() throws Exception {
        assumeNotNull(groupDnResolver);
    }

    private String name = "Mail";
    private String[] path = new String[] {"GoogleApps", "Cru", "Cru"};

    @BeforeClass
    public static void beforeClass()
    {
        groupDnResolver = new GroupValueTranscoder();
        groupDnResolver.setBaseDn(baseDn);
    }

    @Test
    public void testGroupDnResolver() throws Exception {
        assumeConfigured();

        String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps," + groupDnResolver.getBaseDn();

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }

    @Test
    public void testGroupDnResolverCaseInsensitiveDn() throws Exception {
        assumeConfigured();

        String groupDn = "CN=" + name + ",OU=Cru,ou=Cru,OU=GoogleApps," +
                groupDnResolver.getBaseDn().toUpperCase();

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));

        assertTrue(groupDn.equalsIgnoreCase(groupDnResolver.encodeStringValue(group)));
    }

    @Test
    public void testGroupNotMutated() throws Exception {
        assumeConfigured();

        String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps," + groupDnResolver.getBaseDn();

        Group group = groupDnResolver.decodeStringValue(groupDn);

        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
        assertEquals(groupDn, groupDnResolver.encodeStringValue(group));
    }
}
