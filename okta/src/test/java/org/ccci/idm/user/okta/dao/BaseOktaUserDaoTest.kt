package org.ccci.idm.user.okta.dao

import com.nhaarman.mockitokotlin2.mock
import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.User
import org.junit.Before
import org.mockito.Mockito.RETURNS_DEEP_STUBS

abstract class BaseOktaUserDaoTest {
    protected lateinit var okta: Client
    protected lateinit var dao: OktaUserDao

    protected lateinit var oktaUser: User

    @Before
    fun setupDao() {
        okta = mock()
        dao = OktaUserDao(okta)
    }

    @Before
    fun setupOktaMocks() {
        oktaUser = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    }

    protected companion object {
        const val INVALID_PASSWORD_MESSAGE = "INVALID_PASSWORD_MESSAGE"

        val invalidPasswordError = mapOf(
            "errorCode" to "E0000001",
            "errorSummary" to "Api validation failed: password",
            "errorCauses" to listOf(mapOf("errorSummary" to "password: $INVALID_PASSWORD_MESSAGE"))
        )
    }
}
