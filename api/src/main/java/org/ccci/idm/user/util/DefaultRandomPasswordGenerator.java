package org.ccci.idm.user.util;

import java.security.SecureRandom;

public class DefaultRandomPasswordGenerator implements RandomPasswordGenerator {
    private static final SecureRandom RAND = new SecureRandom();

    private String validCharacters = "abcdefghjkmnpqrstuvwxyz23456789";

    private int length = 8;

    public void setValidCharacters(final String characters) {
        this.validCharacters = characters;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    @Override
    public String generatePassword() {
        final StringBuilder result = new StringBuilder();

        int range = this.validCharacters.length();
        for (int i = 0; i < this.length; i++) {
            result.append(this.validCharacters.charAt(RAND.nextInt(range)));
        }

        return result.toString();
    }
}
