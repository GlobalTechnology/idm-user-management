package org.ccci.idm.user.okta.dao

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.EmailStatus
import com.okta.sdk.resource.user.UserBuilder
import org.ccci.idm.user.Group
import org.ccci.idm.user.SearchQuery
import org.ccci.idm.user.User
import org.ccci.idm.user.dao.AbstractUserDao
import org.ccci.idm.user.exception.UserNotFoundException
import org.ccci.idm.user.okta.OktaGroup
import org.ccci.idm.user.okta.dao.util.filterUsers
import org.ccci.idm.user.okta.dao.util.oktaUserId
import org.ccci.idm.user.okta.dao.util.searchUsers
import org.ccci.idm.user.query.Attribute
import org.ccci.idm.user.query.BooleanExpression
import org.ccci.idm.user.query.ComparisonExpression
import org.ccci.idm.user.query.Expression
import java.util.EnumSet
import java.util.concurrent.BlockingQueue
import java.util.stream.Stream

private const val PROFILE_EMAIL = "email"
private const val PROFILE_FIRST_NAME = "firstName"
private const val PROFILE_LAST_NAME = "lastName"
private const val PROFILE_THEKEY_GUID = "theKeyGuid"
private const val PROFILE_RELAY_GUID = "relayGuid"
private const val PROFILE_US_EMPLOYEE_ID = "usEmployeeId"
private const val PROFILE_US_DESIGNATION = "usDesignationNumber"
private const val PROFILE_NICK_NAME = "nickName"
private const val PROFILE_EMAIL_ALIASES = "emailAliases"

private val DEFAULT_ATTRS = arrayOf(User.Attr.EMAIL, User.Attr.NAME, User.Attr.FLAGS)

class OktaUserDao(private val okta: Client, private val listeners: List<Listener>? = null) : AbstractUserDao() {
    var initialGroups: Set<String> = emptySet()

    private fun findOktaUser(user: User) =
        findOktaUserByOktaUserId(user.oktaUserId) ?: findOktaUserByTheKeyGuid(user.theKeyGuid)

    fun findByOktaUserId(id: String?) = findOktaUserByOktaUserId(id)?.asIdmUser()
    private fun findOktaUserByOktaUserId(id: String?) = id?.let { okta.getUser(id) }

    override fun findByEmail(email: String?, includeDeactivated: Boolean): User? {
        if (email == null) return null
        return okta.filterUsers("profile.email eq \"$email\"").firstOrNull()?.asIdmUser()
    }

    override fun findByTheKeyGuid(guid: String?, includeDeactivated: Boolean) =
        findOktaUserByTheKeyGuid(guid)?.asIdmUser()
    private fun findOktaUserByTheKeyGuid(guid: String?) =
        guid?.let { okta.searchUsers("profile.$PROFILE_THEKEY_GUID eq \"$guid\"").firstOrNull() }

    override fun findByRelayGuid(guid: String?, includeDeactivated: Boolean): User? {
        if (guid == null) return null
        return okta.searchUsers("profile.$PROFILE_RELAY_GUID eq \"$guid\"").firstOrNull()?.asIdmUser()
    }

    override fun streamUsers(expression: Expression?, deactivated: Boolean, restrict: Boolean): Stream<User> {
        return okta.listUsers(null, null, null, expression?.toOktaExpression(), null).stream()
            .map { it.asIdmUser(loadGroups = false) }
    }

    // region CRUD methods
    override fun save(user: User) {
        assertWritable()
        assertValidUser(user)

        UserBuilder.instance()
            .putProfileProperty(PROFILE_THEKEY_GUID, user.theKeyGuid)
            .putProfileProperty(PROFILE_RELAY_GUID, user.relayGuid)
            .setEmail(user.email)
            .setPassword(user.password.toCharArray())
            .setFirstName(user.firstName)
            .setLastName(user.lastName)
            .putProfileProperty(PROFILE_US_EMPLOYEE_ID, user.employeeId)
            .putProfileProperty(PROFILE_US_DESIGNATION, user.cruDesignation)
            .putProfileProperty(PROFILE_NICK_NAME, user.rawPreferredName)
            .putProfileProperty(PROFILE_EMAIL_ALIASES, user.cruProxyAddresses.toList())
            .setGroups(initialGroups)
            .buildAndCreate(okta)
            .also { user.oktaUserId = it.id }

        listeners?.onEach { it.onUserCreated(user) }
    }

    override fun update(user: User, vararg attrs: User.Attr) {
        assertWritable()
        assertValidUser(user)

        // only update Okta if we are updating attributes tracked in Okta
        val attrsSet = EnumSet.noneOf(User.Attr::class.java).apply { addAll(attrs.ifEmpty { DEFAULT_ATTRS }) }
        if (
            attrsSet.contains(User.Attr.EMAIL) || attrsSet.contains(User.Attr.PASSWORD) ||
            attrsSet.contains(User.Attr.NAME) || attrsSet.contains(User.Attr.CRU_PREFERRED_NAME)
        ) {
            val oktaUser = findOktaUser(user) ?: throw UserNotFoundException()

            var changed = false
            attrsSet.forEach {
                when (it) {
                    User.Attr.EMAIL -> {
                        oktaUser.profile.login = user.email
                        oktaUser.profile.email = user.email
                        changed = true
                    }
                    User.Attr.PASSWORD -> {
                        oktaUser.credentials.password.value = user.password.toCharArray()
                        changed = true
                    }
                    User.Attr.NAME -> {
                        oktaUser.profile.firstName = user.firstName
                        oktaUser.profile[PROFILE_NICK_NAME] = user.rawPreferredName
                        oktaUser.profile.lastName = user.lastName
                        changed = true
                    }
                    User.Attr.CRU_PREFERRED_NAME -> {
                        oktaUser.profile[PROFILE_NICK_NAME] = user.rawPreferredName
                        changed = true
                    }
                    // we don't care about these attributes anymore
                    User.Attr.DOMAINSVISITED,
                    User.Attr.FACEBOOK,
                    User.Attr.FLAGS,
                    User.Attr.GLOBALREGISTRY,
                    User.Attr.LOGINTIME,
                    User.Attr.SELFSERVICEKEYS,
                    User.Attr.MFA_SECRET,
                    User.Attr.MFA_INTRUDER_DETECTION -> Unit
                }
            }

            if (changed) oktaUser.update()
        }

        listeners?.onEach { it.onUserUpdated(user, *attrsSet.toTypedArray()) }
    }
    // endregion CRUD methods

    // region Group methods
    override fun getAllGroups(baseSearch: String?) = okta.listGroups().asSequence()
        .map { it.asIdmGroup() }
        .filter { baseSearch == null || it.isDescendantOfOrEqualTo(baseSearch) }
        .toList()

    override fun addToGroup(user: User, group: Group) {
        require(group is OktaGroup) { "$group is not an Okta Group" }

        val oktaUser = findOktaUser(user) ?: throw UserNotFoundException()
        oktaUser.addToGroup(group.id)
    }

    override fun removeFromGroup(user: User, group: Group) {
        require(group is OktaGroup) { "$group is not an Okta Group" }

        val oktaUserId = user.oktaUserId ?: findOktaUser(user)?.id ?: throw UserNotFoundException()
        okta.getGroup(group.id)?.removeUser(oktaUserId)
    }
    // endregion Group methods

    // region Unsupported Deprecated Methods
    override fun enqueueAll(queue: BlockingQueue<User>, deactivated: Boolean) = throw UnsupportedOperationException()
    override fun findAllByGroup(group: Group, includeDeactivated: Boolean) = throw UnsupportedOperationException()
    override fun findAllByQuery(query: SearchQuery) = throw UnsupportedOperationException()
    // endregion Unsupported Deprecated Methods

    // region Unused methods
    @Deprecated("guids are no longer used and not present within Okta. find users by the key guid instead.")
    override fun findByGuid(guid: String?, includeDeactivated: Boolean) = null
    override fun findByDesignation(designation: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun findByEmployeeId(employeeId: String?, includeDeactivated: Boolean) = TODO("not implemented")
    override fun findByFacebookId(id: String?, includeDeactivated: Boolean) = TODO("not implemented")
    // endregion Unused methods

    private fun com.okta.sdk.resource.user.User.asIdmUser(loadGroups: Boolean = true): User {
        return User().apply {
            oktaUserId = id
            email = profile.email
            isEmailVerified =
                credentials.emails.firstOrNull { email.equals(it.value, true) }?.status == EmailStatus.VERIFIED
            firstName = profile.firstName
            preferredName = profile.getString(PROFILE_NICK_NAME)
            lastName = profile.lastName
            theKeyGuid = profile.getString(PROFILE_THEKEY_GUID)
            relayGuid = profile.getString(PROFILE_RELAY_GUID)
            employeeId = profile.getString(PROFILE_US_EMPLOYEE_ID)
            cruDesignation = profile.getString(PROFILE_US_DESIGNATION)
            cruProxyAddresses = profile.getStringList(PROFILE_EMAIL_ALIASES).orEmpty()

            if (loadGroups) setGroups(listGroups().map { it.asIdmGroup() })
        }.also { user -> listeners?.onEach { it.onUserLoaded(user) } }
    }

    private fun com.okta.sdk.resource.group.Group.asIdmGroup() = OktaGroup(id, profile.name)

    public interface Listener {
        fun onUserLoaded(user: User) = Unit
        fun onUserCreated(user: User) = Unit
        fun onUserUpdated(user: User, vararg attrs: User.Attr) = Unit
    }
}

// region Search Expression processing
private fun Expression.toOktaExpression(): String = when (this) {
    is BooleanExpression -> toOktaExpression()
    is ComparisonExpression -> toOktaExpression()
    else -> throw IllegalArgumentException("Unrecognized Expression: $this")
}

private fun BooleanExpression.toOktaExpression() = when (type) {
    BooleanExpression.Type.AND -> "(${components.joinToString(" and ") { it.toOktaExpression() }})"
    BooleanExpression.Type.OR -> "(${components.joinToString(" or ") { it.toOktaExpression() }})"
}

private fun ComparisonExpression.toOktaExpression() = when {
    attribute == Attribute.GROUP -> TODO("Group search not implemented yet")
    type == ComparisonExpression.Type.EQ -> """${attribute.toOktaProfileAttribute()} eq "$value""""
    type == ComparisonExpression.Type.SW -> """${attribute.toOktaProfileAttribute()} sw "$value""""
    type == ComparisonExpression.Type.LIKE -> throw UnsupportedOperationException("LIKE is unsupported for OktaUserDao")
    else -> throw IllegalArgumentException("Unrecognized ComparisonExpression: $this")
}

private fun Attribute.toOktaProfileAttribute() = when (this) {
    Attribute.GUID -> "profile.$PROFILE_THEKEY_GUID"
    Attribute.EMAIL -> "profile.$PROFILE_EMAIL"
    Attribute.EMAIL_ALIAS -> "profile.$PROFILE_EMAIL_ALIASES"
    Attribute.FIRST_NAME -> "profile.$PROFILE_FIRST_NAME"
    Attribute.LAST_NAME -> "profile.$PROFILE_LAST_NAME"
    Attribute.US_EMPLOYEE_ID -> "profile.$PROFILE_US_EMPLOYEE_ID"
    Attribute.US_DESIGNATION -> "profile.$PROFILE_US_DESIGNATION"
    Attribute.GROUP -> throw IllegalArgumentException("GROUP isn't an actual attribute")
}
// endregion Search Expression processing
