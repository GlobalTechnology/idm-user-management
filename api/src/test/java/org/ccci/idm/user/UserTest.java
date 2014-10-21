package org.ccci.idm.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Locale;
import java.util.UUID;

public class UserTest {
    private static String guid() {
        return UUID.randomUUID().toString().toUpperCase(Locale.US);
    }

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

    public void testClone() throws Exception {
        // create and populate a User object
        final User user = new User();
        user.setGuid(guid());
        user.setRelayGuid(guid());
        user.setTheKeyGuid(guid());
        user.setEmail("test@example.com");
        user.setPassword("p@ssw0rd");
        user.setEmailVerified(true);
        user.setForcePasswordChange(true);
        user.setDeactivated(true);
        user.setLoginDisabled(true);

        // test cloning object
        final User duplicate = user.clone();
        assertEquals(user.getGuid(), duplicate.getGuid());
        assertEquals(user.getRelayGuid(), duplicate.getRelayGuid());
        assertEquals(user.getTheKeyGuid(), duplicate.getTheKeyGuid());
        assertEquals(user.getEmail(), duplicate.getEmail());
        assertEquals(user.getPassword(), duplicate.getPassword());
        assertEquals(user.isEmailVerified(), duplicate.isEmailVerified());
        assertEquals(user.isForcePasswordChange(), duplicate.isForcePasswordChange());
        assertEquals(user.isDeactivated(), duplicate.isDeactivated());
        assertEquals(user.isLoginDisabled(), duplicate.isLoginDisabled());
    }
}
