package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.mapper.GroupDnResolver;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assume.assumeNotNull;

import static org.junit.Assert.assertEquals;

public class GroupDnResolverTest
{
    private static GroupDnResolver groupDnResolver;
    private static final String baseDn = "ou=groups,ou=idm,dc=cru,dc=org";

    private void assumeConfigured() throws Exception {
        assumeNotNull(groupDnResolver);
    }

    private String name = "Mail";
    private String[] path = new String[] {"GoogleApps", "Cru", "Cru"};

    @BeforeClass
    public static void beforeClass()
    {
        groupDnResolver = new GroupDnResolver();
        groupDnResolver.setBaseDn(baseDn);
    }

    @Test
    public void testGroupDnResolver() throws Exception {
        assumeConfigured();

        String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps," + groupDnResolver.getBaseDn();

        Group group = groupDnResolver.resolve(groupDn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));

        assertEquals(groupDn, groupDnResolver.resolve(group));
    }

    @Test
    public void testGroupNotMutated() throws Exception {
        assumeConfigured();

        String groupDn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps," + groupDnResolver.getBaseDn();

        Group group = groupDnResolver.resolve(groupDn);

        assertEquals(groupDn, groupDnResolver.resolve(group));
        assertEquals(groupDn, groupDnResolver.resolve(group));
    }
}
