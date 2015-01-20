package org.ccci.idm.user.dao;

import static org.ccci.idm.user.TestUtil.newUser;
import static org.junit.Assert.fail;

import org.ccci.idm.user.dao.exception.ReadOnlyDaoException;
import org.junit.Test;

public abstract class AbstractUserDaoTest {
    protected abstract AbstractUserDao getUserDao();

    @Test
    public void testReadOnly() throws Exception {
        final AbstractUserDao dao = this.getUserDao();
        dao.setReadOnly(true);

        // test save user
        try {
            dao.save(newUser());
            fail("ReadOnlyDaoException not thrown for UserDao.save(User)");
        } catch (final ReadOnlyDaoException e) {
            // expected
        }

        // test update a user
        try {
            dao.update(newUser());
            fail("ReadOnlyDaoException not thrown for UserDao.update(User)");
        } catch (final ReadOnlyDaoException e) {
            // expected
        }

        // test updating a user 2
        try {
            dao.update(newUser(), newUser());
            fail("ReadOnlyDaoException not thrown for UserDao.update(User, User)");
        } catch (final ReadOnlyDaoException e) {
            // expected
        }

        // test adding a user to a group
        try {
            dao.addToGroup(newUser(), null);
            fail("ReadOnlyDaoException not thrown for UserDao.addToGroup(User, Group)");
        } catch (final ReadOnlyDaoException e) {
            // expected
        }

        // test removing a user from a group
        try {
            dao.removeFromGroup(newUser(), null);
            fail("ReadOnlyDaoException not thrown for UserDao.removeFromGroup(User, Group)");
        } catch (final ReadOnlyDaoException e) {
            // expected
        }
    }
}
