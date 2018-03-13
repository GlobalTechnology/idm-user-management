package org.ccci.idm.user.util;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class UserUtil {
    private static final Pattern VALID_GUID =
            Pattern.compile("^[0-9a-f]{8}(?:-[0-9a-f]{4}){4}[0-9a-f]{8}$", Pattern.CASE_INSENSITIVE);

    public static boolean isValidGuid(@Nullable final String guid) {
        return guid != null && VALID_GUID.matcher(guid).matches();
    }
}
