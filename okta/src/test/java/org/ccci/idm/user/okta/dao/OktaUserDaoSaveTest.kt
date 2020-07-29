package org.ccci.idm.user.okta.dao

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.whenever
import com.okta.sdk.impl.error.DefaultError
import com.okta.sdk.resource.ResourceException
import com.okta.sdk.resource.user.PasswordCredential
import com.okta.sdk.resource.user.UserCredentials
import org.ccci.idm.user.User
import org.ccci.idm.user.exception.InvalidPasswordUserException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID
import com.okta.sdk.resource.user.User as OktaUser

class OktaUserDaoSaveTest : BaseOktaUserDaoTest() {
    @Before
    fun setupMocksForSave() {
        okta.stub {
            on { instantiate(OktaUser::class.java) } doReturn oktaUser
            on { instantiate(UserCredentials::class.java) } doReturn mock()
            on { instantiate(PasswordCredential::class.java) } doReturn mock()
        }
        whenever(oktaUser.profile).thenReturn(mock())
    }

    @Test
    fun testInvalidPasswordUserException() {
        val user = User().apply {
            email = "test@example.com"
            theKeyGuid = UUID.randomUUID().toString()
            relayGuid = theKeyGuid
            firstName = "Test"
            lastName = "User"
            password = "a"
        }
        whenever(okta.createUser(any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(ResourceException(DefaultError(invalidPasswordError)))

        try {
            dao.save(user)
            fail("creating a user with an invalid password didn't throw an exception")
        } catch (e: InvalidPasswordUserException) {
            assertEquals(INVALID_PASSWORD_MESSAGE, e.message)
        }
    }
}
