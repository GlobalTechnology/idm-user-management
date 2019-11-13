package org.ccci.idm.user

import java.io.Serializable

interface Group : Serializable {
    val id: String?
    val name: String?
}
