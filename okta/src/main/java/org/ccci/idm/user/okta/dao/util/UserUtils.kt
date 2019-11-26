package org.ccci.idm.user.okta.dao.util

import org.ccci.idm.user.User

private const val OKTA_USER_ID = "oktaUserId"

internal var User.oktaUserId: String?
    get() = getImplMeta(OKTA_USER_ID, String::class.java)
    set(value) {
        setImplMeta(OKTA_USER_ID, value)
    }
