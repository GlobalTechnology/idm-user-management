package org.ccci.idm.user.okta.dao

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.okta.sdk.impl.error.DefaultError
import com.okta.sdk.resource.ResourceException
import org.ccci.idm.user.User
import org.ccci.idm.user.exception.InvalidPasswordUserException
import org.ccci.idm.user.okta.dao.util.oktaUserId
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

private const val OKTA_USER_ID = "oktaUserId"

class OktaUserDaoUpdateUserTest : BaseOktaUserDaoTest() {
    private val user = User().apply {
        oktaUserId = OKTA_USER_ID
        email = "test@example.com"
        theKeyGuid = UUID.randomUUID().toString()
        relayGuid = UUID.randomUUID().toString()
    }

    @Before
    fun setup() {
        whenever(okta.getUser(OKTA_USER_ID)).thenReturn(oktaUser)
    }

    @Test
    fun testInvalidPasswordUserException() {
        whenever(oktaUser.update()).thenThrow(ResourceException(DefaultError(invalidPasswordError)))

        user.password = "a"
        try {
            dao.update(user, User.Attr.PASSWORD)
            fail("updating an invalid password didn't throw an exception")
        } catch (e: InvalidPasswordUserException) {
            assertEquals(INVALID_PASSWORD_MESSAGE, e.message)
        }
        verify(oktaUser).update()
    }
}
