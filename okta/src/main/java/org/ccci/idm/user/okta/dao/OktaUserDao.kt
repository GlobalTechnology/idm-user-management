package org.ccci.idm.user.okta.dao

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.EmailStatus
import com.okta.sdk.resource.user.UserBuilder
import org.ccci.idm.user.Group
import org.ccci.idm.user.SearchQuery
import org.ccci.idm.user.User
import org.ccci.idm.user.dao.UserDao
import org.ccci.idm.user.okta.dao.util.filterUsers
import org.ccci.idm.user.okta.dao.util.oktaUserId
import org.ccci.idm.user.okta.dao.util.searchUsers
import org.ccci.idm.user.query.Expression
import java.util.concurrent.BlockingQueue
import com.okta.sdk.resource.user.User as OktaUser

private const val PROFILE_THEKEY_GUID = "theKeyGuid"
private const val PROFILE_RELAY_GUID = "relayGuid"
private const val PROFILE_US_EMPLOYEE_ID = "usEmployeeId"
private const val PROFILE_US_DESIGNATION = "usDesignationNumber"
private const val PROFILE_EMAIL_ALIASES = "emailAliases"

class OktaUserDao(private val okta: Client) : UserDao {
    override fun isReadOnly() = true

    fun findByOktaUserId(id: String?): User? {
        if (id == null) return null
        return okta.getUser(id)?.toIdmUser()
    }

    override fun findByEmail(email: String?, includeDeactivated: Boolean): User? {
        if (email == null) return null
        return okta.filterUsers("profile.email eq \"$email\"").firstOrNull()?.toIdmUser()
    }

    override fun findByTheKeyGuid(guid: String?, includeDeactivated: Boolean): User? {
        if (guid == null) return null
        return okta.searchUsers("profile.$PROFILE_THEKEY_GUID eq \"$guid\"").firstOrNull()?.toIdmUser()
    }

    override fun findByRelayGuid(guid: String?, includeDeactivated: Boolean): User? {
        if (guid == null) return null
        return okta.searchUsers("profile.$PROFILE_RELAY_GUID eq \"$guid\"").firstOrNull()?.toIdmUser()
    }

    // region CRUD methods
    override fun save(user: User) {
        UserBuilder.instance()
            .putProfileProperty(PROFILE_THEKEY_GUID, user.theKeyGuid)
            .putProfileProperty(PROFILE_RELAY_GUID, user.relayGuid)
            .setEmail(user.email)
            .setPassword(user.password.toCharArray())
            .setFirstName(user.firstName)
            .setLastName(user.lastName)
            .putProfileProperty(PROFILE_US_EMPLOYEE_ID, user.employeeId)
            .putProfileProperty(PROFILE_US_DESIGNATION, user.cruDesignation)
            .putProfileProperty(PROFILE_EMAIL_ALIASES, user.cruProxyAddresses.toList())
            .buildAndCreate(okta)
    }
    // endregion CRUD methods

    // region Unsupported CRUD methods
    override fun update(original: User, user: User, vararg attrs: User.Attr?) = throw UnsupportedOperationException()
    override fun update(user: User?, vararg attrs: User.Attr?) = throw UnsupportedOperationException()
    override fun addToGroup(user: User, group: Group) = throw UnsupportedOperationException()
    override fun addToGroup(user: User, group: Group, addSecurity: Boolean) = throw UnsupportedOperationException()
    override fun removeFromGroup(user: User, group: Group) = throw UnsupportedOperationException()
    // endregion Unsupported CRUD methods

    // region Unsupported Deprecated Methods
    override fun enqueueAll(queue: BlockingQueue<User>, deactivated: Boolean) = throw UnsupportedOperationException()
    override fun findAllByGroup(group: Group, includeDeactivated: Boolean) = throw UnsupportedOperationException()
    override fun findAllByQuery(query: SearchQuery) = throw UnsupportedOperationException()
    // endregion Unsupported Deprecated Methods

    // region Unused methods
    override fun findByDesignation(designation: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun findByEmployeeId(employeeId: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun findByFacebookId(id: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun findByGuid(guid: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun getAllGroups(baseSearch: String?) = TODO("not implemented")
    override fun streamUsers(expression: Expression?, deactivated: Boolean, restrict: Boolean) = TODO("not implemented")
    // endregion Unused methods

    private fun OktaUser.toIdmUser(): User {
        return User().apply {
            oktaUserId = id
            email = profile.email
            isEmailVerified =
                credentials.emails.firstOrNull { email.equals(it.value, true) }?.status == EmailStatus.VERIFIED
            firstName = profile.firstName
            lastName = profile.lastName
            theKeyGuid = profile.getString(PROFILE_THEKEY_GUID)
            relayGuid = profile.getString(PROFILE_RELAY_GUID)
            employeeId = profile.getString(PROFILE_US_EMPLOYEE_ID)
            cruDesignation = profile.getString(PROFILE_US_DESIGNATION)
            cruProxyAddresses = profile.getStringList(PROFILE_EMAIL_ALIASES)
        }
    }
}
