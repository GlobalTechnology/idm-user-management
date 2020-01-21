package org.ccci.idm.user.okta

import org.ccci.idm.user.Group

data class OktaGroup(
    override val id: String? = null,
    val oktaGroupType: String? = null
) : Group {
    constructor(
        id: String? = null,
        name: String? = null,
        oktaGroupType: String? = null
    ) : this(id = id, oktaGroupType = oktaGroupType) {
        this.name = name
    }

    override var name: String? = null

    fun isDescendantOfOrEqualTo(prefix: String) = name == prefix || name?.startsWith("$prefix-") == true
}
