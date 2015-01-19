package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.dao.AbstractUserDaoTest;

public class LdaptiveUserDaoTest extends AbstractUserDaoTest<LdaptiveUserDao> {
    @Override
    protected LdaptiveUserDao getUserDao() {
        return new LdaptiveUserDao();
    }
}
