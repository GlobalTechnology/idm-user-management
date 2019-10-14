package org.ccci.idm.user.dao

import org.ccci.idm.user.User
import org.ccci.idm.user.dao.exception.ReadOnlyDaoException
import org.springframework.util.Assert

abstract class AbstractUserDao : UserDao {
    final override var isReadOnly = false

    protected fun assertWritable() {
        throw ReadOnlyDaoException()
    }

    protected fun assertValidUser(user: User) {
        Assert.notNull(user, "No user was provided")
        Assert.hasText(user.email, "E-mail address cannot be blank.")
        Assert.hasText(user.guid, "GUID cannot be blank")
    }
}
