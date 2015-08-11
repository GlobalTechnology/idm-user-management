package org.ccci.idm.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

@RunWith(Parameterized.class)
public class UserSecurityAnswerTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        final ImmutableList.Builder<Object[]> data = ImmutableList.builder();
        data.add(new Object[]{null, false, new String[0], new String[]{null, "null", "", " "}});
        data.add(new Object[]{"null", true, new String[]{" null ", "null"}, new String[]{null, "", " "}});
        data.add(new Object[]{"", false, new String[0], new String[]{null, "null", "", " "}});
        data.add(new Object[]{" ", false, new String[0], new String[]{null, "null", "", " "}});
        data.add(new Object[]{" A   b C ", true, new String[]{"a b c", " A   b C ", "A    B       c   "}, new
                String[]{null, "null", "", " ", " ab c"}});
        return data.build();
    }

    private final String answer;
    private final boolean set;
    private final String[] valid;
    private final String[] invalid;

    public UserSecurityAnswerTest(final String answer, final boolean set, final String[] valid,
                                  final String[] invalid) {
        this.answer = answer;
        this.set = set;
        this.valid = valid;
        this.invalid = invalid;
    }

    @Test
    public void testSecurityAnswer() throws Exception {
        final User user = new User();
        user.setSecurityAnswer(this.answer);
        assertEquals(set, user.hasSecurityAnswer());
        for (final String answer : valid) {
            assertTrue(user.checkSecurityAnswer(answer));
        }
        for (final String answer : invalid) {
            assertFalse(user.checkSecurityAnswer(answer));
        }
    }
}