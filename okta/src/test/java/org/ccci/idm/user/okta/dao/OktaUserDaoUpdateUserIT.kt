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

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration("/config.xml")
class OktaUserDaoUpdateUserIT {
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
        val user = dao.findByEmail("daniel.frett+okta11@gmail.com", false)!!
        user.password = "a"
        dao.update(user, User.Attr.PASSWORD)
    }
}
