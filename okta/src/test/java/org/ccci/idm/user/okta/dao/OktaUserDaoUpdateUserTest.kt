package org.ccci.idm.user.okta.dao

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.okta.sdk.client.Client
import com.okta.sdk.impl.error.DefaultError
import com.okta.sdk.resource.ResourceException
import org.ccci.idm.user.User
import org.ccci.idm.user.exception.InvalidPasswordUserException
import org.ccci.idm.user.okta.dao.util.oktaUserId
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.util.UUID
import com.okta.sdk.resource.user.User as OktaUser

private const val OKTA_USER_ID = "oktaUserId"
private const val INVALID_PASSWORD_MESSAGE = "INVALID_PASSWORD_MESSAGE"

class OktaUserDaoUpdateUserTest {
    private lateinit var client: Client
    private lateinit var dao: OktaUserDao

    private lateinit var oktaUser: OktaUser
    private val user = User().apply {
        oktaUserId = OKTA_USER_ID
        email = "test@example.com"
        theKeyGuid = UUID.randomUUID().toString()
        relayGuid = UUID.randomUUID().toString()
    }

    @Before
    fun setup() {
        oktaUser = mock(defaultAnswer = RETURNS_DEEP_STUBS)
        client = mock {
            whenever(it.getUser(OKTA_USER_ID)) doReturn oktaUser
        }
        dao = OktaUserDao(client)
    }

    @Test
    fun testInvalidPasswordUserException() {
        val error = mapOf(
            "errorCode" to "E0000001",
            "errorSummary" to "Api validation failed: password",
            "errorCauses" to listOf(mapOf("errorSummary" to INVALID_PASSWORD_MESSAGE))
        )
        whenever(oktaUser.update()) doThrow ResourceException(DefaultError(error))

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
