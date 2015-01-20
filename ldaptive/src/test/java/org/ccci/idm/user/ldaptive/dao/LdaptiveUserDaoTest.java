package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.dao.AbstractUserDaoTest;

public class LdaptiveUserDaoTest extends AbstractUserDaoTest {
    @Override
    protected LdaptiveUserDao getUserDao() {
        return new LdaptiveUserDao();
    }
}
