package org.ccci.idm.user.okta

import org.ccci.idm.user.Group

data class OktaGroup(override val id: String? = null) : Group {
    constructor(
        id: String? = null,
        oktaGroupType: String? = null,
        name: String? = null
    ) : this(id = id) {
        this.oktaGroupType = oktaGroupType
        this.name = name
    }

    var oktaGroupType: String? = null
        private set
    override var name: String? = null
        private set

    fun isDescendantOfOrEqualTo(prefix: String) = name == prefix || name?.startsWith("$prefix-") == true
}
