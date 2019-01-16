package org.ccci.idm.user.util;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class UserUtil {
    private static final Pattern VALID_GUID =
            Pattern.compile("^[0-9a-f]{8}(?:-[0-9a-f]{4}){4}[0-9a-f]{8}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern VALID_US_DESIGNATION = Pattern.compile("^[0-9]{7}$");
    private static final Pattern VALID_US_EMPLOYEE_ID = Pattern.compile("^[0-9]{9}[SD]?$");

    public static boolean isValidGuid(@Nullable final String guid) {
        return guid != null && VALID_GUID.matcher(guid).matches();
    }

    public static boolean isValidUsDesignation(@Nullable final String designation) {
        return designation != null && VALID_US_DESIGNATION.matcher(designation).matches();
    }

    public static boolean isValidUsEmployeeId(@Nullable final String employeeId) {
        return employeeId != null && VALID_US_EMPLOYEE_ID.matcher(employeeId).matches();
    }
}
