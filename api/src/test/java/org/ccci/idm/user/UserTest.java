package org.ccci.idm.user;

import static org.ccci.idm.user.TestUtil.guid;
import static org.ccci.idm.user.TestUtil.randomEmail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

public class UserTest {
    private Random RAND = new SecureRandom();

    @Ignore
    @Test
    public void testGuidFallbacks() throws Exception {
        final User user = new User();

        // test behavior for non-existant Key & Relay guids
        {
            final String guid = guid();
            user.setGuid(guid);
            user.setRelayGuid(null);
            user.setTheKeyGuid(null);

            assertEquals(guid, user.getGuid());
            assertNull(user.getRawTheKeyGuid());
            assertEquals(guid, user.getTheKeyGuid());
            assertNull(user.getRawRelayGuid());
            assertEquals(guid, user.getRelayGuid());
        }

        // test Key override guid
        {
            final String guid = guid();
            final String keyGuid = guid();
            user.setGuid(guid);
            user.setTheKeyGuid(keyGuid);
            user.setRelayGuid(null);

            assertEquals(guid, user.getGuid());
            assertEquals(keyGuid, user.getRawTheKeyGuid());
            assertEquals(keyGuid, user.getTheKeyGuid());
            assertNull(user.getRawRelayGuid());
            assertEquals(guid, user.getRelayGuid());
        }

        // test Relay override guid
        {
            final String guid = guid();
            final String relayGuid = guid();
            user.setGuid(guid);
            user.setTheKeyGuid(null);
            user.setRelayGuid(relayGuid);

            assertEquals(guid, user.getGuid());
            assertNull(user.getRawTheKeyGuid());
            assertEquals(guid, user.getTheKeyGuid());
            assertEquals(relayGuid, user.getRawRelayGuid());
            assertEquals(relayGuid, user.getRelayGuid());
        }

        // test Relay & The Key override guids
        {
            final String guid = guid();
            final String keyGuid = guid();
            final String relayGuid = guid();
            user.setGuid(guid);
            user.setTheKeyGuid(keyGuid);
            user.setRelayGuid(relayGuid);

            assertEquals(guid, user.getGuid());
            assertEquals(keyGuid, user.getRawTheKeyGuid());
            assertEquals(keyGuid, user.getTheKeyGuid());
            assertEquals(relayGuid, user.getRawRelayGuid());
            assertEquals(relayGuid, user.getRelayGuid());
        }
    }

    @Test
    public void testClone() throws Exception {
        // iterate several times to try cloning multiple random objects
        for (int count = 0; count < 100; count++) {
            // create and populate a User object with random values
            final User user = new User();
//            user.setGuid(guid());
            user.setRelayGuid(guid());
            user.setTheKeyGuid(guid());

            // use random guid as random strings
            user.setEmail(randomEmail());
            user.setPassword(guid());
            user.setFirstName(guid());
            user.setLastName(guid());

            // set random flags
            user.setEmailVerified(RAND.nextBoolean());
            user.setAllowPasswordChange(RAND.nextBoolean());
            user.setForcePasswordChange(RAND.nextBoolean());
            user.setDeactivated(RAND.nextBoolean());
            user.setLoginDisabled(RAND.nextBoolean());
            user.setLocked(RAND.nextBoolean());

            // populate multi-valued attributes with values
            for(int i = 0; i < 10; i++) {
                user.addDomainsVisited(guid());
            }

            // test cloning object
            final User duplicate = user.clone();
//            assertEquals(user.getGuid(), duplicate.getGuid());
            assertEquals(user.getRelayGuid(), duplicate.getRelayGuid());
            assertEquals(user.getTheKeyGuid(), duplicate.getTheKeyGuid());
            assertEquals(user.getEmail(), duplicate.getEmail());
            assertEquals(user.getPassword(), duplicate.getPassword());
            assertEquals(user.getFirstName(), duplicate.getFirstName());
            assertEquals(user.getLastName(), duplicate.getLastName());
            assertEquals(user.isEmailVerified(), duplicate.isEmailVerified());
            assertEquals(user.isAllowPasswordChange(), duplicate.isAllowPasswordChange());
            assertEquals(user.isForcePasswordChange(), duplicate.isForcePasswordChange());
            assertEquals(user.isDeactivated(), duplicate.isDeactivated());
            assertEquals(user.isLoginDisabled(), duplicate.isLoginDisabled());
            assertEquals(user.isLocked(), duplicate.isLocked());

            assertTrue(CollectionUtils.isEqualCollection(user.getDomainsVisited(), duplicate.getDomainsVisited()));
            assertTrue(CollectionUtils.isEqualCollection(user.getGroups(), duplicate.getGroups()));
        }
    }

    @Test
    public void testSetEmail() throws Exception {
        final User user = new User();

        // test setting email resets email verified flag
        {
            // test behavior when no verified flag is specified
            user.setEmailVerified(true);
            assertTrue(user.isEmailVerified());
            user.setEmail(randomEmail());
            assertFalse(user.isEmailVerified());

            // test behavior when verified flag is specified
            user.setEmailVerified(true);
            assertTrue(user.isEmailVerified());
            user.setEmail(randomEmail(), false);
            assertFalse(user.isEmailVerified());
            user.setEmail(randomEmail(), true);
            assertTrue(user.isEmailVerified());
        }

        // test preserving emailVerified flag when setting email to same address or changing case of email
        {
            user.setEmail(randomEmail());
            assert user.getEmail() != null;
            user.setEmailVerified(true);
            assertTrue(user.isEmailVerified());
            user.setEmail(user.getEmail());
            assertTrue(user.isEmailVerified());
            user.setEmail(user.getEmail().toUpperCase(Locale.US));
            assertTrue(user.isEmailVerified());
            user.setEmail(user.getEmail().toLowerCase(Locale.US));
            assertTrue(user.isEmailVerified());

            user.setEmailVerified(false);
            assertFalse(user.isEmailVerified());
            user.setEmail(user.getEmail());
            assertFalse(user.isEmailVerified());
            user.setEmail(user.getEmail().toUpperCase(Locale.US));
            assertFalse(user.isEmailVerified());
            user.setEmail(user.getEmail().toLowerCase(Locale.US));
            assertFalse(user.isEmailVerified());
        }
    }
}
