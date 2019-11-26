package org.ccci.idm.user.okta.dao.util

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.UserList

fun Client.filterUsers(filter: String): UserList = listUsers(null, filter, null, null, null)
fun Client.searchUsers(q: String): UserList = listUsers(null, null, null, q, null)
