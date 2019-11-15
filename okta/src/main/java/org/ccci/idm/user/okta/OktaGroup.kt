package org.ccci.idm.user.okta

import org.ccci.idm.user.Group

data class OktaGroup(
    override val id: String? = null
) : Group {
    constructor(id: String? = null, name: String? = null) : this(id) {
        this.name = name
    }

    override var name: String? = null

    fun isDescendantOfOrEqualTo(prefix: String) = name == prefix || name?.startsWith("$prefix-") == true
}
