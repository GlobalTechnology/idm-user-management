package org.ccci.idm.user.okta.dao.exception

import com.okta.sdk.resource.ResourceException
import org.ccci.idm.user.dao.exception.DaoException

class OktaDaoException(e: ResourceException) : DaoException(e) {
    val error = e.error
}
