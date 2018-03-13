package org.ccci.idm.user.util;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.ccci.idm.user.util.UserUtil.isValidGuid;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(JUnitParamsRunner.class)
public class UserUtilTest {
    @Test
    public void verifyIsValidGuidValid() {
        for(int i = 0; i < 100; i++) {
            final String guid = UUID.randomUUID().toString();
            assertTrue(guid + " should be a valid guid", isValidGuid(guid));
            assertTrue(guid.toUpperCase() + " should be a valid guid", isValidGuid(guid.toUpperCase()));
            assertTrue(guid.toLowerCase() + " should be a valid guid", isValidGuid(guid.toLowerCase()));
        }
    }

    @Test
    @Parameters({
            "null",
            "asdf",
            "01234567-89AB-CDEF-0123-456789ABCDE",
            "01234567-89AB-CDEF-0123-456789ABCDEF0",
            "01234567-89AB-CDEF-0123-456789ABCDEG",
            "0123456789AB-CDEF-0123-4567-89ABCDEF"
    })
    public void verifyIsValidGuidInvalid(@Nullable final String guid) {
        assertFalse(isValidGuid(guid));
    }
}
