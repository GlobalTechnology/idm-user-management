package org.ccci.idm.user.okta.dao.listeners

import com.okta.authn.sdk.client.AuthenticationClient
import org.ccci.idm.user.User
import org.ccci.idm.user.okta.dao.OktaUserDao
import java.lang.Thread.sleep

class OktaPasswordHackForCreateUserListener(
    private val authClient: AuthenticationClient,
    private val sleep: Long = 3000
) : OktaUserDao.Listener {
    override fun onUserCreated(user: User) {
        // HACK: When a new Okta user is created it will trigger provisioning of the account to eDirectory.
        //       Unfortunately it generates a new random password for the LDAP account and actually provisions the
        //       password on the next login.
        //       In addition, if we authenticate the user before the LDAP provisioning completes the password isn't
        //       pushed to the provisioned account, this is the reason for the sleep.
        sleep(sleep)
        authClient.authenticate(user.email, user.password.toCharArray(), null, null)
    }
}
