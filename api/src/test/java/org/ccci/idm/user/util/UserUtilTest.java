package org.ccci.idm.user.util;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.ccci.idm.user.util.UserUtil.isValidGuid;
import static org.ccci.idm.user.util.UserUtil.isValidUsDesignation;
import static org.ccci.idm.user.util.UserUtil.isValidUsEmployeeId;

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
        for (int i = 0; i < 100; i++) {
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

    @Test
    @Parameters({
            "0123456",
            "1111111",
            "0000000"
    })
    public void verifyIsValidUsDesignationValid(final String designation) {
        assertTrue(isValidUsDesignation(designation));
    }

    @Test
    @Parameters({
            "null",
            "012345",
            "11111111",
            "abcdefg",
            "111111a"
    })
    public void verifyIsValidUsDesignationInvalid(@Nullable final String designation) {
        assertFalse(isValidUsDesignation(designation));
    }

    @Test
    @Parameters({
            "012345678",
            "111111111",
            "111111111S",
            "111111111D"
    })
    public void verifyIsValidUsEmployeeIdValid(final String employeeId) {
        assertTrue(isValidUsEmployeeId(employeeId));
    }

    @Test
    @Parameters({
            "null",
            "abcdefghi",
            "01234567",
            "1111111111",
            "111111111s",
            "111111111d",
            "111111111T"
    })
    public void verifyIsValidUsEmployeeIdInvalid(@Nullable final String employeeId) {
        assertFalse(isValidUsEmployeeId(employeeId));
    }
}
