package org.ccci.idm.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Locale;
import java.util.UUID;

public class UserTest {
    @Test
    public void testGuidFallbacks() {
        final User user = new User();

        // test behavior for non-existant Key & Relay guids
        {
            final String guid = UUID.randomUUID().toString().toUpperCase(Locale.US);
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
            final String guid = UUID.randomUUID().toString().toUpperCase(Locale.US);
            final String keyGuid = UUID.randomUUID().toString().toUpperCase(Locale.US);
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
            final String guid = UUID.randomUUID().toString().toUpperCase(Locale.US);
            final String relayGuid = UUID.randomUUID().toString().toUpperCase(Locale.US);
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
            final String guid = UUID.randomUUID().toString().toUpperCase(Locale.US);
            final String keyGuid = UUID.randomUUID().toString().toUpperCase(Locale.US);
            final String relayGuid = UUID.randomUUID().toString().toUpperCase(Locale.US);
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
}
