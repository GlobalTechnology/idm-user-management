package org.ccci.idm.user.ldaptive.dao.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.ccci.idm.user.Group;
import org.junit.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.NoOpDnResolver;

import java.util.Collection;

public class UserLdapEntryMapperTest {
    private UserLdapEntryMapper getMapper() {
        final UserLdapEntryMapper mapper = new UserLdapEntryMapper();
        mapper.setDnResolver(new NoOpDnResolver());
        return mapper;
    }

    @Test
    public void testNullGroupDnResolver() throws Exception {
        // get mapper
        final UserLdapEntryMapper mapper = this.getMapper();

        // create entry with a couple groups
        final LdapEntry entry = new LdapEntry();
        final String name = "attr_group";
        entry.addAttribute(new LdapAttribute(name, "cn=test,ou=groups1", "cn=test2,ou=groups2"));

        // test resolution using no Group DN resolver
        {
            mapper.setGroupDnResolver(null);
            final Collection<Group> groups = mapper.getGroupValues(entry, name);
            assertNotNull(groups);
            assertEquals(0, groups.size());
        }

        // test resolution using a base Group DN resolver
        {
            mapper.setGroupDnResolver(new GroupDnResolver());
            final Collection<Group> groups = mapper.getGroupValues(entry, name);
            assertNotNull(groups);
            assertEquals(2, groups.size());
        }
    }
}
