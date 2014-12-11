package org.ccci.idm.user.ldaptive.dao.mapper;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASS_PERSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder;
import org.junit.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.NoOpDnResolver;

import java.util.Collection;
import java.util.UUID;

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
            mapper.setGroupValueTranscoder(null);
            final Collection<Group> groups = mapper.getGroupValues(entry, name);
            assertNotNull(groups);
            assertEquals(0, groups.size());
        }

        // test resolution using a base Group DN resolver
        {
            mapper.setGroupValueTranscoder(new GroupValueTranscoder());
            final Collection<Group> groups = mapper.getGroupValues(entry, name);
            assertNotNull(groups);
            assertEquals(2, groups.size());
        }
    }

    @Test
    public void testFutureObjectClassCompatibility() throws Exception {
        final UserLdapEntryMapper mapper = this.getMapper();

        // generate a couple "future" objectClasses that will never actually exist
        final String objectClass1 = "Class_" + UUID.randomUUID().toString();
        final String objectClass2 = "Class_" + UUID.randomUUID().toString();

        // test entry with a single "future" objectClass
        {
            final LdapEntry entry1 = new LdapEntry();
            entry1.addAttribute(mapper.attr(LDAP_ATTR_OBJECTCLASS, ImmutableSet.of(objectClass1)));

            // map to user & back to entry
            final User user = new User();
            final LdapEntry entry2 = new LdapEntry();
            mapper.map(entry1, user);
            mapper.map(user, entry2);

            // extract result object classes
            final LdapAttribute attr = entry2.getAttribute(LDAP_ATTR_OBJECTCLASS);
            final Collection<String> result = attr.getStringValues();
            assertTrue(result.contains(objectClass1));
            assertFalse(result.contains(objectClass2));
        }

        // test entry with multiple "future" objectClasses
        {
            final LdapEntry entry1 = new LdapEntry();
            entry1.addAttribute(mapper.attr(LDAP_ATTR_OBJECTCLASS, ImmutableSet.of(objectClass1, objectClass2,
                    LDAP_OBJECTCLASS_PERSON)));

            // map to user & back to entry
            final User user = new User();
            final LdapEntry entry2 = new LdapEntry();
            mapper.map(entry1, user);
            mapper.map(user, entry2);

            // extract result object classes
            final LdapAttribute attr = entry2.getAttribute(LDAP_ATTR_OBJECTCLASS);
            final Collection<String> result = attr.getStringValues();
            assertTrue(result.contains(objectClass1));
            assertTrue(result.contains(objectClass2));
        }
    }
}
