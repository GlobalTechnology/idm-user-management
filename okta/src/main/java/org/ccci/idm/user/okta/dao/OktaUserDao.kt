package org.ccci.idm.user.okta.dao

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.EmailStatus
import com.okta.sdk.resource.user.UserBuilder
import org.ccci.idm.user.Group
import org.ccci.idm.user.SearchQuery
import org.ccci.idm.user.User
import org.ccci.idm.user.dao.AbstractUserDao
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException
import org.ccci.idm.user.exception.GroupNotFoundException
import org.ccci.idm.user.exception.UserNotFoundException
import org.ccci.idm.user.okta.OktaGroup
import org.ccci.idm.user.okta.dao.util.filterUsers
import org.ccci.idm.user.okta.dao.util.oktaUserId
import org.ccci.idm.user.okta.dao.util.searchUsers
import org.ccci.idm.user.query.Attribute
import org.ccci.idm.user.query.BooleanExpression
import org.ccci.idm.user.query.ComparisonExpression
import org.ccci.idm.user.query.Expression
import org.ccci.idm.user.query.NotExpression
import org.joda.time.Instant
import java.util.EnumSet
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

private const val PROFILE_THEKEY_GUID = "theKeyGuid"
private const val PROFILE_RELAY_GUID = "relayGuid"
private const val PROFILE_EMAIL = "email"
private const val PROFILE_FIRST_NAME = "firstName"
private const val PROFILE_NICK_NAME = "nickName"
private const val PROFILE_LAST_NAME = "lastName"

private const val PROFILE_PHONE_NUMBER = "primaryPhone"
private const val PROFILE_CITY = "city"
private const val PROFILE_STATE = "state"
private const val PROFILE_ZIP_CODE = "zipCode"
private const val PROFILE_COUNTRY = "cruCountryCode"

private const val PROFILE_US_EMPLOYEE_ID = "usEmployeeId"
private const val PROFILE_US_DESIGNATION = "usDesignationNumber"

private const val PROFILE_ORGANIZATION = "organization"
private const val PROFILE_DIVISION = "division"
private const val PROFILE_DEPARTMENT = "department"
private const val PROFILE_MANAGER_ID = "managerId"

private const val PROFILE_ORIGINAL_EMAIL = "original_email"
private const val PROFILE_EMAIL_ALIASES = "emailAliases"

private const val PROFILE_GR_MASTER_PERSON_ID = "grMasterPersonId"
private const val PROFILE_GR_PERSON_ID = "thekeyGrPersonId"

private val DEFAULT_ATTRS = arrayOf(User.Attr.EMAIL, User.Attr.NAME, User.Attr.FLAGS)
private const val DEACTIVATED_PREFIX = "\$GUID-"
private const val DEACTIVATED_SUFFIX = "@deactivated.cru.org"

class OktaUserDao(private val okta: Client, private val listeners: List<Listener>? = null) : AbstractUserDao() {
    var maxSearchResults = SEARCH_NO_LIMIT
    var initialGroups: Set<String> = emptySet()

    private fun findOktaUser(user: User) =
        findOktaUserByOktaUserId(user.oktaUserId) ?: findOktaUserByTheKeyGuid(user.theKeyGuid)

    fun findByOktaUserId(id: String?) = findOktaUserByOktaUserId(id)?.asIdmUser()
    private fun findOktaUserByOktaUserId(id: String?) = id?.let { okta.getUser(id) }

    override fun findByEmail(email: String?, includeDeactivated: Boolean) = when {
        email == null -> null
        includeDeactivated ->
            okta.searchUsers("""profile.$PROFILE_EMAIL eq "$email" or profile.$PROFILE_ORIGINAL_EMAIL eq "$email"""")
        else -> okta.filterUsers("""profile.$PROFILE_EMAIL eq "$email"""")
    }?.firstOrNull()?.asIdmUser()

    override fun findByTheKeyGuid(guid: String?, includeDeactivated: Boolean) =
        findOktaUserByTheKeyGuid(guid)?.asIdmUser()?.takeIf { !it.isDeactivated || includeDeactivated }
    private fun findOktaUserByTheKeyGuid(guid: String?) =
        guid?.let { okta.searchUsers("profile.$PROFILE_THEKEY_GUID eq \"$guid\"").firstOrNull() }

    override fun findByRelayGuid(guid: String?, includeDeactivated: Boolean) =
        guid?.let { okta.searchUsers("profile.$PROFILE_RELAY_GUID eq \"$guid\"").firstOrNull()?.asIdmUser() }
            ?.takeIf { !it.isDeactivated || includeDeactivated }

    // region Stream Users
    override fun streamUsers(
        expression: Expression?,
        includeDeactivated: Boolean,
        restrictMaxAllowed: Boolean
    ): Stream<User> {
        val search = expression?.toOktaExpression(includeDeactivated)
        return okta.listUsers(null, null, null, search, null).stream()
            .restrictMaxAllowed(restrictMaxAllowed)
            .map { it.asIdmUser(loadGroups = false) }
    }

    override fun streamUsersInGroup(
        group: Group,
        expression: Expression?,
        includeDeactivated: Boolean,
        restrictMaxAllowed: Boolean
    ): Stream<User> {
        require(group is OktaGroup) { "OktaGroup is required for streamUsersInGroup" }
        val oktaGroup = group.id?.let { okta.getGroup(it) } ?: throw GroupNotFoundException()

        return oktaGroup.listUsers().stream()
            .filter { it.matches(expression) }
            .restrictMaxAllowed(restrictMaxAllowed)
            .map { it.asIdmUser(loadGroups = false) }
    }

    private fun <T> Stream<T>.restrictMaxAllowed(restrict: Boolean = true) =
        if (restrict && maxSearchResults != SEARCH_NO_LIMIT) {
            val count = AtomicInteger(0)
            peek {
                if (count.incrementAndGet() > maxSearchResults)
                    throw ExceededMaximumAllowedResultsException("Search exceeded $maxSearchResults results")
            }
        } else this
    // endregion Stream Users

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
            .putProfileProperty(PROFILE_NICK_NAME, user.rawPreferredName)
            .setLastName(user.lastName)
            .putProfileProperty(PROFILE_US_EMPLOYEE_ID, user.employeeId)
            .putProfileProperty(PROFILE_US_DESIGNATION, user.cruDesignation)
            .putProfileProperty(PROFILE_PHONE_NUMBER, user.telephoneNumber)

            // Location profile attributes
            .putProfileProperty(PROFILE_CITY, user.city)
            .putProfileProperty(PROFILE_STATE, user.state)
            .putProfileProperty(PROFILE_ZIP_CODE, user.postal)
            .putProfileProperty(PROFILE_COUNTRY, user.country)

            // HR profile attributes
            .putProfileProperty(PROFILE_ORGANIZATION, user.cruMinistryCode)
            .putProfileProperty(PROFILE_DIVISION, user.cruSubMinistryCode)
            .putProfileProperty(PROFILE_DEPARTMENT, user.departmentNumber)
            .putProfileProperty(PROFILE_MANAGER_ID, user.cruManagerID)

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
            attrsSet.contains(User.Attr.NAME) || attrsSet.contains(User.Attr.CRU_PREFERRED_NAME) ||
            attrsSet.contains(User.Attr.CONTACT) || attrsSet.contains(User.Attr.LOCATION) ||
            attrsSet.contains(User.Attr.EMPLOYEE_NUMBER) || attrsSet.contains(User.Attr.CRU_DESIGNATION) ||
            attrsSet.contains(User.Attr.HUMAN_RESOURCE) || attrsSet.contains(User.Attr.CRU_PROXY_ADDRESSES)
        ) {
            val oktaUser = findOktaUser(user) ?: throw UserNotFoundException()

            var changed = false
            attrsSet.forEach {
                when (it) {
                    User.Attr.EMAIL -> {
                        if (user.isDeactivated) {
                            oktaUser.profile.email = "$DEACTIVATED_PREFIX${user.theKeyGuid}$DEACTIVATED_SUFFIX"
                            oktaUser.profile[PROFILE_ORIGINAL_EMAIL] = user.email
                        } else {
                            oktaUser.profile.email = user.email
                            oktaUser.profile[PROFILE_ORIGINAL_EMAIL] = null
                        }
                        oktaUser.profile.login = oktaUser.profile.email
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
                    User.Attr.CONTACT -> {
                        oktaUser.profile[PROFILE_PHONE_NUMBER] = user.telephoneNumber
                        changed = true
                    }
                    User.Attr.LOCATION -> {
                        oktaUser.profile[PROFILE_CITY] = user.city
                        oktaUser.profile[PROFILE_STATE] = user.state
                        oktaUser.profile[PROFILE_ZIP_CODE] = user.postal
                        oktaUser.profile[PROFILE_COUNTRY] = user.country
                        changed = true
                    }
                    User.Attr.EMPLOYEE_NUMBER -> {
                        oktaUser.profile[PROFILE_US_EMPLOYEE_ID] = user.employeeId
                        changed = true
                    }
                    User.Attr.CRU_DESIGNATION -> {
                        oktaUser.profile[PROFILE_US_DESIGNATION] = user.cruDesignation
                        changed = true
                    }
                    User.Attr.HUMAN_RESOURCE -> {
                        oktaUser.profile[PROFILE_ORGANIZATION] = user.cruMinistryCode
                        oktaUser.profile[PROFILE_DIVISION] = user.cruSubMinistryCode
                        oktaUser.profile[PROFILE_DEPARTMENT] = user.departmentNumber
                        oktaUser.profile[PROFILE_MANAGER_ID] = user.cruManagerID
                        changed = true
                    }
                    User.Attr.CRU_PROXY_ADDRESSES -> {
                        oktaUser.profile[PROFILE_EMAIL_ALIASES] = user.cruProxyAddresses.toList()
                        changed = true
                    }
                    // these attributes are still tracked in LDAP but not in Okta
                    User.Attr.FLAGS,
                    User.Attr.SECURITYQA,
                    User.Attr.SELFSERVICEKEYS,
                    User.Attr.MFA_SECRET,
                    User.Attr.MFA_INTRUDER_DETECTION -> Unit
                    // we don't care about these attributes at all anymore
                    User.Attr.DOMAINSVISITED,
                    User.Attr.FACEBOOK,
                    User.Attr.GLOBALREGISTRY,
                    User.Attr.LOGINTIME -> Unit
                }
            }

            if (changed) oktaUser.update()
        }

        listeners?.onEach { it.onUserUpdated(user, *attrsSet.toTypedArray()) }
    }

    override fun reactivate(user: User) {
        val oktaUser = findOktaUser(user) ?: return
        super.reactivate(user)
        oktaUser.unsuspend()
    }

    override fun deactivate(user: User) {
        val oktaUser = findOktaUser(user) ?: return
        oktaUser.suspend()
        super.deactivate(user)
    }
    // endregion CRUD methods

    // region Group methods
    override fun getGroup(groupId: String?) = groupId?.let { okta.getGroup(groupId) }?.asIdmGroup()

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
            theKeyGuid = profile.getString(PROFILE_THEKEY_GUID)
            relayGuid = profile.getString(PROFILE_RELAY_GUID)

            isDeactivated = profile.email.startsWith(DEACTIVATED_PREFIX) && profile.email.endsWith(DEACTIVATED_SUFFIX)
            email = if (isDeactivated) profile.getString(PROFILE_ORIGINAL_EMAIL) else profile.email
            isEmailVerified =
                credentials.emails.firstOrNull { email.equals(it.value, true) }?.status == EmailStatus.VERIFIED

            firstName = profile.firstName
            preferredName = profile.getString(PROFILE_NICK_NAME)
            lastName = profile.lastName
            telephoneNumber = profile.getString(PROFILE_PHONE_NUMBER)

            // location profile attributes
            city = profile.getString(PROFILE_CITY)
            state = profile.getString(PROFILE_STATE)
            postal = profile.getString(PROFILE_ZIP_CODE)
            country = profile.getString(PROFILE_COUNTRY)

            // HR profile attributes
            cruMinistryCode = profile.getString(PROFILE_ORGANIZATION)
            cruSubMinistryCode = profile.getString(PROFILE_DIVISION)
            departmentNumber = profile.getString(PROFILE_DEPARTMENT)
            cruManagerID = profile.getString(PROFILE_MANAGER_ID)

            employeeId = profile.getString(PROFILE_US_EMPLOYEE_ID)
            cruDesignation = profile.getString(PROFILE_US_DESIGNATION)
            cruProxyAddresses = profile.getStringList(PROFILE_EMAIL_ALIASES).orEmpty()

            grMasterPersonId = profile.getString(PROFILE_GR_MASTER_PERSON_ID)
            grPersonId = profile.getString(PROFILE_GR_PERSON_ID)

            loginTime = lastLogin?.let { Instant(it.time) }

            if (loadGroups) setGroups(listGroups().map { it.asIdmGroup() })
        }.also { user -> listeners?.onEach { it.onUserLoaded(user) } }
    }

    private fun com.okta.sdk.resource.group.Group.asIdmGroup() = OktaGroup(id, profile.name)

    interface Listener {
        fun onUserLoaded(user: User) = Unit
        fun onUserCreated(user: User) = Unit
        fun onUserUpdated(user: User, vararg attrs: User.Attr) = Unit
    }
}

// region Search Expression processing
private fun Expression.toOktaExpression(includeDeactivated: Boolean): String = when (this) {
    is BooleanExpression -> toOktaExpression(includeDeactivated)
    is ComparisonExpression -> toOktaExpression(includeDeactivated)
    else -> throw IllegalArgumentException("Unrecognized Expression: $this")
}

private fun BooleanExpression.toOktaExpression(includeDeactivated: Boolean) = when (type) {
    BooleanExpression.Type.AND -> "(${components.joinToString(" and ") { it.toOktaExpression(includeDeactivated) }})"
    BooleanExpression.Type.OR -> "(${components.joinToString(" or ") { it.toOktaExpression(includeDeactivated) }})"
}

private fun ComparisonExpression.toOktaExpression(includeDeactivated: Boolean): String = when {
    attribute == Attribute.GROUP -> TODO("Group search not implemented yet")
    includeDeactivated && attribute == Attribute.EMAIL ->
        "(${oktaComparisonExpression(PROFILE_EMAIL, type, value)} or " +
            "${oktaComparisonExpression(PROFILE_ORIGINAL_EMAIL, type, value)})"
    else -> oktaComparisonExpression(attribute.toOktaProfileAttribute(), type, value)
}

private fun Attribute.toOktaProfileAttribute() = when (this) {
    Attribute.GUID -> PROFILE_THEKEY_GUID
    Attribute.EMAIL -> PROFILE_EMAIL
    Attribute.EMAIL_ALIAS -> PROFILE_EMAIL_ALIASES
    Attribute.FIRST_NAME -> PROFILE_FIRST_NAME
    Attribute.LAST_NAME -> PROFILE_LAST_NAME
    Attribute.US_EMPLOYEE_ID -> PROFILE_US_EMPLOYEE_ID
    Attribute.US_DESIGNATION -> PROFILE_US_DESIGNATION
    Attribute.GROUP -> throw IllegalArgumentException("GROUP isn't an actual attribute")
}

private fun oktaComparisonExpression(attr: String, oper: ComparisonExpression.Type, value: String?) =
    """profile.$attr ${oper.toOktaOper()} "$value""""

private fun ComparisonExpression.Type.toOktaOper() = when (this) {
    ComparisonExpression.Type.EQ -> "eq"
    ComparisonExpression.Type.SW -> "sw"
    ComparisonExpression.Type.LIKE -> throw UnsupportedOperationException("LIKE is unsupported for OktaUserDao")
}

private fun com.okta.sdk.resource.user.User.matches(expression: Expression?): Boolean = when (expression) {
    is BooleanExpression -> matches(expression)
    is ComparisonExpression -> matches(expression) == true
    is NotExpression -> !matches(expression.component)
    else -> true
}

private fun com.okta.sdk.resource.user.User.matches(expression: BooleanExpression) = when (expression.type) {
    BooleanExpression.Type.AND -> expression.components.all { matches(it) }
    BooleanExpression.Type.OR -> expression.components.any { matches(it) }
}

private fun com.okta.sdk.resource.user.User.matches(expression: ComparisonExpression): Boolean? {
    if (expression.attribute == Attribute.EMAIL_ALIAS) {
        val aliases = profile?.getStringList(PROFILE_EMAIL_ALIASES)
        return when (expression.type) {
            ComparisonExpression.Type.EQ -> aliases?.any { it.equals(expression.value.orEmpty(), true) }
            ComparisonExpression.Type.SW -> aliases?.any { it.startsWith(expression.value.orEmpty(), true) }
            else -> false
        }
    }

    val value: String? = when (expression.attribute) {
        Attribute.GUID -> profile?.getString(PROFILE_THEKEY_GUID)
        Attribute.EMAIL -> profile?.email
        Attribute.FIRST_NAME -> profile?.firstName
        Attribute.LAST_NAME -> profile?.lastName
        Attribute.US_EMPLOYEE_ID -> profile?.getString(PROFILE_US_EMPLOYEE_ID)
        Attribute.US_DESIGNATION -> profile?.getString(PROFILE_US_DESIGNATION)
        else -> return false
    }

    return when (expression.type) {
        ComparisonExpression.Type.EQ -> value.equals(expression.value.orEmpty(), true)
        ComparisonExpression.Type.SW -> value?.startsWith(expression.value.orEmpty(), true)
        else -> false
    }
}
// endregion Search Expression processing
