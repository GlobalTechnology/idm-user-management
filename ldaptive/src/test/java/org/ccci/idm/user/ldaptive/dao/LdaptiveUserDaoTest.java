package org.ccci.idm.user.ldaptive.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ccci.idm.user.dao.AbstractUserDaoTest;
import org.junit.Before;
import org.junit.Test;

public class LdaptiveUserDaoTest extends AbstractUserDaoTest {
    private LdaptiveUserDao dao;

    @Before
    public void setup() {
        dao = new LdaptiveUserDao();
    }

    @Override
    protected LdaptiveUserDao getUserDao() {
        return dao;
    }

    @Test
    public void testCalculatePageSize() {
        dao.setMaxPageSize(10);
        dao.setMaxSearchResults(5);

        assertThat(dao.calculatePageSize(1, false), is(1));
        assertThat(dao.calculatePageSize(10, false), is(10));
        assertThat(dao.calculatePageSize(20, false), is(10));
        assertThat(dao.calculatePageSize(1, true), is(1));
        assertThat(dao.calculatePageSize(5, true), is(5));
        assertThat(dao.calculatePageSize(10, true), is(5 + 1));
    }
}
