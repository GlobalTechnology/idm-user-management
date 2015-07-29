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
    private static final String DELIMITER = "-----";

    public static final Integer MAX_HISTORY = 8;

    private Ordering<String> reverseChronologicalOrdering = new Ordering<String>() {
        @Override
        public int compare(String first, String second) {
            try {
                DateTime firstDateTime = new DateTime(Iterables.getLast(Splitter.on(DELIMITER).split(first)));
                DateTime secondDateTime = new DateTime(Iterables.getLast(Splitter.on(DELIMITER).split(second)));

                if (firstDateTime.isBefore(secondDateTime)) {
                    return 1;
                } else if (firstDateTime.isAfter(secondDateTime)) {
                    return -1;
                }
            } catch (Exception e) {
                // do nothing
            }

            return 0;
        }
    };

    public void add(String password, Collection<String> history) {
        if (history.size() >= MAX_HISTORY) {
            List<String> list = sort(history, reverseChronologicalOrdering);
            history.clear();
            history.addAll(list.subList(0, MAX_HISTORY - 1));
        }

        history.add(BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_WORK_FACTOR)) + DELIMITER + new DateTime());
    }

    private List<String> sort(Collection<String> collection, Ordering<String> ordering) {
        List<String> list = Lists.newArrayList(collection);
        Collections.sort(list, this.reverseChronologicalOrdering);
        return list;
    }

    private Function<String, String> before(final String word) {
        return new Function<String, String>()
        {
            public String apply(String from)
            {
                return Iterables.getFirst(Splitter.on(word).split(from), from);
            }
        };
    }

    public Boolean isPasswordHistorical(String password, Collection<String> history) {
        return isHashed(password, Lists.transform(Lists.newArrayList(history), before(DELIMITER)));
    }

    private boolean isHashed(String string, List<String> hashes)
    {
        for (String hash : hashes)
            if (BCrypt.checkpw(string, hash))
                return true;

        return false;
    }

}
