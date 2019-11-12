package org.ccci.idm.user.okta.dao

import org.ccci.idm.user.User
import org.ccci.idm.user.dao.UserDao

private val UPDATABLE_ATTRS = setOf(User.Attr.MFA_SECRET, User.Attr.MFA_INTRUDER_DETECTION, User.Attr.SELFSERVICEKEYS)

class FallbackDaoOktaUserDaoListener(
    private val dao: UserDao
) : OktaUserDao.Listener {
    override fun onUserLoaded(user: User) {
        dao.findByTheKeyGuid(user.theKeyGuid, true)?.run {
            // MFA attributes
            user.isMfaBypassed = isMfaBypassed
            user.mfaEncryptedSecret = mfaEncryptedSecret
            user.isMfaIntruderLocked = isMfaIntruderLocked
            user.mfaIntruderAttempts = mfaIntruderAttempts
            user.mfaIntruderResetTime = mfaIntruderResetTime

            // self-service keys
            user.signupKey = signupKey
            user.proposedEmail = proposedEmail
            user.changeEmailKey = changeEmailKey
            user.resetPasswordKey = resetPasswordKey
        }
    }

    override fun onUserUpdated(user: User, vararg attrs: User.Attr) {
        val filteredAttrs = attrs.filter { UPDATABLE_ATTRS.contains(it) }
        if (filteredAttrs.isEmpty()) return

        val original = dao.findByTheKeyGuid(user.theKeyGuid, true) ?: return
        dao.update(original, user, *filteredAttrs.toTypedArray())
    }
}
