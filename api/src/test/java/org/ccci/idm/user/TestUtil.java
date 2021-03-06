package org.ccci.idm.user;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class TestUtil {
    private static final Random RAND = new SecureRandom();

    @Nonnull
    public static String guid() {
        return UUID.randomUUID().toString().toUpperCase(Locale.US);
    }

    @Nonnull
    public static User newUser() {
        final User user = new User();
        user.setEmail(randomEmail());
        user.setGuid(guid());
        user.setTheKeyGuid(guid());
        user.setRelayGuid(guid());
        user.setPassword(guid());
        user.setFirstName(randomName());
        user.setLastName(randomName());
        return user;
    }

    @Nonnull
    public static String randomEmail() {
        return "test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com";
    }

    @Nonnull
    public static String randomName() {
        return "Name-" + RAND.nextInt(Integer.MAX_VALUE);
    }
}
