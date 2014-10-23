package org.ccci.idm.user;

import static org.ccci.idm.user.Constants.STRENGTH_FULL;
import static org.ccci.idm.user.Constants.STRENGTH_NONE;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class User implements Cloneable, Serializable {
    private String email;
    private String password;

    private String guid;
    private String theKeyGuid;
    private String relayGuid;

    private String firstName;
    private String lastName;

    // account flags
    private boolean emailVerified = false;
    private boolean allowPasswordChange = true;
    private boolean forcePasswordChange = false;
    private boolean deactivated = false;
    private boolean loginDisabled = false;
    private boolean locked = false;

    // Multi-value attributes
    private final Collection<String> domainsVisited = new HashSet<String>();
    private final Collection<String> groups = new HashSet<String>();

    // self-service verification keys
    private String signupKey = null;
    private String changeEmailKey = null;
    private String resetPasswordKey = null;
    private String proposedEmail = null;

    // federated identities
    private String facebookId = null;
    private double facebookIdStrength = STRENGTH_NONE;

    // miscellaneous meta-data
    private String deactivatedUid;

    public User() {
    }

    protected User(final User source) {
        this.email = source.email;
        this.password = source.password;
        this.guid = source.guid;
        this.theKeyGuid = source.theKeyGuid;
        this.relayGuid = source.relayGuid;
        this.firstName = source.firstName;
        this.lastName = source.lastName;
        this.emailVerified = source.emailVerified;
        this.allowPasswordChange = source.allowPasswordChange;
        this.forcePasswordChange = source.forcePasswordChange;
        this.deactivated = source.deactivated;
        this.loginDisabled = source.loginDisabled;
        this.locked = source.locked;

        this.domainsVisited.addAll(source.domainsVisited);
        this.groups.addAll(source.groups);

        this.signupKey = source.signupKey;
        this.changeEmailKey = source.changeEmailKey;
        this.resetPasswordKey = source.resetPasswordKey;
        this.proposedEmail = source.proposedEmail;

        this.facebookId = source.facebookId;
        this.facebookIdStrength = source.facebookIdStrength;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getGuid() {
        return this.guid;
    }

    public void setGuid(final String guid) {
        this.guid = guid;
    }

    public String getTheKeyGuid() {
        return this.theKeyGuid != null ? this.theKeyGuid : this.guid;
    }

    public String getRawTheKeyGuid() {
        return this.theKeyGuid;
    }

    public void setTheKeyGuid(final String guid) {
        this.theKeyGuid = guid;
    }

    public String getRelayGuid() {
        return this.relayGuid != null ? this.relayGuid : this.guid;
    }

    public String getRawRelayGuid() {
        return this.relayGuid;
    }

    public void setRelayGuid(final String guid) {
        this.relayGuid = guid;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    public void setEmailVerified(final boolean verified) {
        this.emailVerified = verified;
    }

    public boolean isAllowPasswordChange() {
        return allowPasswordChange;
    }

    public void setAllowPasswordChange(final boolean allow) {
        this.allowPasswordChange = allow;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(final boolean force) {
        this.forcePasswordChange = force;
    }

    public boolean isDeactivated() {
        return this.deactivated;
    }

    public void setDeactivated(final boolean deactivated) {
        this.deactivated = deactivated;
    }

    public boolean isLoginDisabled() {
        return this.loginDisabled;
    }

    public void setLoginDisabled(final boolean disabled) {
        this.loginDisabled = disabled;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    /**
     * @return the domainsVisited
     */
    public Collection<String> getDomainsVisited() {
        return Collections.unmodifiableCollection(this.domainsVisited);
    }

    /**
     * @param domains a collection of domains visited
     */
    public void setDomainsVisited(final Collection<String> domains) {
        this.domainsVisited.clear();
        if (domains != null) {
            this.domainsVisited.addAll(Collections2.filter(domains, Predicates.notNull()));
        }
    }

    public void addDomainsVisited(final String domain) {
        if (StringUtils.hasText(domain)) {
            this.domainsVisited.add(domain);
        }
    }

    /**
     * @param groups the groups to set
     */
    public void setGroups(final Collection<String> groups) {
        this.groups.clear();
        if (groups != null) {
            this.groups.addAll(groups);
        }
    }

    /**
     * @return the groupMembership
     */
    public Collection<String> getGroups() {
        return Collections.unmodifiableCollection(this.groups);
    }

    public String getSignupKey() {
        return this.signupKey;
    }

    public void setSignupKey(final String key) {
        this.signupKey = key;
    }

    public String getChangeEmailKey() {
        return this.changeEmailKey;
    }

    public void setChangeEmailKey(final String key) {
        this.changeEmailKey = key;
    }

    public String getResetPasswordKey() {
        return this.resetPasswordKey;
    }

    public void setResetPasswordKey(final String key) {
        this.resetPasswordKey = key;
    }

    public String getProposedEmail() {
        return this.proposedEmail;
    }

    public void setProposedEmail(final String email) {
        this.proposedEmail = email;
    }

    public void setFacebookId(final String id, final Number strength) {
        this.facebookId = id;
        this.facebookIdStrength = (strength != null ? strength.doubleValue() : STRENGTH_NONE);
        if (this.facebookIdStrength < STRENGTH_NONE) {
            this.facebookIdStrength = STRENGTH_NONE;
        } else if (this.facebookIdStrength > STRENGTH_FULL) {
            this.facebookIdStrength = STRENGTH_FULL;
        }
    }

    public String getFacebookId() {
        return this.facebookId;
    }

    public Double getFacebookIdStrengthFor(final String id) {
        if (id != null && id.equals(this.facebookId)) {
            return this.facebookIdStrength;
        }
        return STRENGTH_NONE;
    }

    public void removeFacebookId(final String id) {
        if (id != null && id.equals(this.facebookId)) {
            this.facebookId = null;
            this.facebookIdStrength = STRENGTH_NONE;
        }
    }

    public String getDeactivatedUid() {
        return this.deactivatedUid;
    }

    public void setDeactivatedUid(final String deactivatedUid) {
        this.deactivatedUid = deactivatedUid;
    }

    @Override
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    protected User clone() {
        return new User(this);
    }
}
