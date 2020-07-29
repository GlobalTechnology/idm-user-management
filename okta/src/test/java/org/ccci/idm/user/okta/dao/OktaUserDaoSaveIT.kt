package org.ccci.idm.user.okta.dao

import com.okta.sdk.authc.credentials.TokenClientCredentials
import com.okta.sdk.client.Client
import com.okta.sdk.client.Clients
import org.ccci.idm.user.User
import org.ccci.idm.user.exception.InvalidPasswordUserException
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.Random
import java.util.UUID

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration("/config.xml")
class OktaUserDaoSaveIT {
    @Value("\${okta.orgUrl:#{null}}")
    private var orgUrl: String? = null

    @Value("\${okta.apiKey:#{null}}")
    private var apiKey: String? = null

    private lateinit var client: Client
    private lateinit var dao: OktaUserDao

    @Before
    fun setup() {
        assumeTrue("okta.orgUrl not configured", orgUrl != null)
        assumeTrue("okta.apiKey not configured", apiKey != null)

        client = Clients.builder()
            .setOrgUrl(orgUrl)
            .setClientCredentials(TokenClientCredentials(apiKey))
            .build()
        dao = OktaUserDao(client)
    }

    @Test(expected = InvalidPasswordUserException::class)
    fun testInvalidPasswordUserException() {
        val user = User().apply {
            theKeyGuid = UUID.randomUUID().toString()
            relayGuid = theKeyGuid
            email = "test-${Random().nextInt()}@example.com"
            firstName = "Test"
            lastName = "User"
        }
        user.password = "a"
        dao.save(user)
    }
}
