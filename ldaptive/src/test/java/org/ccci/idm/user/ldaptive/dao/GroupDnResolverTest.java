package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.Group;
import org.ccci.idm.user.ldaptive.dao.mapper.GroupDnResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Arrays;

import static org.junit.Assume.assumeNotNull;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"ldap.xml", "config.xml", "dao-default.xml"})
public class GroupDnResolverTest
{
    @Inject
    private GroupDnResolver groupDnResolver;

    private void assumeConfigured() throws Exception {
        assumeNotNull(groupDnResolver);
    }

    @Test
    public void testGroupDnResolver() throws Exception {
        assumeConfigured();

        String name = "Mail";
        String[] path = new String[] {"GoogleApps", "Cru", "Cru"};
        String dn = "cn=" + name + ",ou=Cru,ou=Cru,ou=GoogleApps," + groupDnResolver.getBaseDn();

        Group group = groupDnResolver.resolve(dn);

        assertEquals(name, group.getName());
        assertEquals(Arrays.toString(path), Arrays.toString(group.getPath()));
        assertEquals(dn, groupDnResolver.resolve(group));
    }
}
