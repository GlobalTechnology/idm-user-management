package org.ccci.idm.user.ldaptive

import com.google.common.base.MoreObjects
import org.ccci.idm.user.Group
import javax.annotation.concurrent.Immutable

@Immutable
data class LdapGroup(val dn: Dn) : Group {
    companion object {
        private const val serialVersionUID = 8588784014544957895L
    }

    init {
        require(Dn.ROOT != dn) { "Invalid DN for a group" }
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .addValue(dn.components.reversed().joinToString(",") { "${it.type}=${it.value}" })
            .toString()
    }
}
