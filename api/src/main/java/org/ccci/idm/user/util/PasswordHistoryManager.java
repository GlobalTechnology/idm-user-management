package org.ccci.idm.user.util;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * author@lee.braddock
 */
public class PasswordHistoryManager {

    private static final Integer BCRYPT_WORK_FACTOR = 12;

    // arbitrary delimiter which should never match any part of a password hash or timestamp
    private static final String HASH_TIME_STAMP_DELIMITER = "-----";

    public static final Integer MAX_HISTORY = 8;

    private static final Splitter HASH_TIME_STAMP_SPLITTER = Splitter.on(HASH_TIME_STAMP_DELIMITER);

    private static final Ordering<String> CHRONOLOGICAL_ORDERING = new Ordering<String>() {
        @Override
        public int compare(String first, String second) {
            try {
                return new DateTime(Iterables.getLast(HASH_TIME_STAMP_SPLITTER.split(second))).
                        compareTo(new DateTime(Iterables.getLast(HASH_TIME_STAMP_SPLITTER.split(first))));
            } catch (Exception e) {
                return 0;
            }
        }
    };

    public List<String> add(String password, Collection<String> history) {
        return add(password, Lists.newArrayList(history));
    }

    public boolean isPasswordHistorical(String password, Collection<String> history) {
        return isHashed(password, Lists.transform(Lists.newArrayList(history), RAW_HASH_FUNCTION));
    }

    private List<String> add(String password, List<String> history) {
        if (history.size() >= MAX_HISTORY) {
            Collections.sort(history, CHRONOLOGICAL_ORDERING);
            history = history.subList(0, MAX_HISTORY - 1);
        }

        history.add(BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_WORK_FACTOR)) + HASH_TIME_STAMP_DELIMITER + new DateTime());

        return history;
    }

    private static Function<String, String> RAW_HASH_FUNCTION = new Function<String, String>() {
        public String apply(String from) {
            return Iterables.getFirst(HASH_TIME_STAMP_SPLITTER.split(from), from);
        }
    };

    private boolean isHashed(String string, List<String> hashes)
    {
        for (String hash : hashes)
            if (BCrypt.checkpw(string, hash))
                return true;

        return false;
    }
}
