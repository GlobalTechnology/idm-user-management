package org.ccci.idm.user.util;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class HashUtilityTest {
    @Test
    public void testCheckInvalidHash() throws Exception {
        assertFalse(HashUtility.checkHash("raw", "notavalidhash"));
    }
}
