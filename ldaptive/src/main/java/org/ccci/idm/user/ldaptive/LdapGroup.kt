package org.ccci.idm.user.ldaptive

import com.google.common.base.MoreObjects
import org.ccci.idm.user.Group
import org.ccci.idm.user.ldaptive.dao.util.DnUtils
import javax.annotation.concurrent.Immutable

@Immutable
data class LdapGroup(val dn: Dn) : Group {
    companion object {
        private const val serialVersionUID = 8588784014544957895L
    }

    constructor(rawDn: String) : this(DnUtils.toDn(rawDn))

    init {
        require(Dn.ROOT != dn) { "Invalid DN for a group" }
    }

    override val name get() = dn.name

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .addValue(DnUtils.toString(dn))
            .toString()
    }
}
