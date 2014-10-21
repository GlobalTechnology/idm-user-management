package org.ccci.idm.user;

import static org.ccci.idm.user.Constants.STRENGTH_FULL;
import static org.ccci.idm.user.Constants.STRENGTH_NONE;

import java.io.Serializable;

public class User implements Cloneable, Serializable {
    private String email;
    private String password;

    private String guid;
    private String theKeyGuid;
    private String relayGuid;

    // account flags
    private boolean emailVerified = false;
    private boolean allowPasswordChange = true;
    private boolean forcePasswordChange = false;
    private boolean deactivated = false;
    private boolean loginDisabled = false;

    // federated identities
    private String facebookId = null;
    private double facebookIdStrength = STRENGTH_NONE;

    public User() {
    }

    protected User(final User source) {
        this.email = source.email;
        this.password = source.password;
        this.guid = source.guid;
        this.theKeyGuid = source.theKeyGuid;
        this.relayGuid = source.relayGuid;
        this.emailVerified = source.emailVerified;
        this.allowPasswordChange = source.allowPasswordChange;
        this.forcePasswordChange = source.forcePasswordChange;
        this.deactivated = source.deactivated;
        this.loginDisabled = source.loginDisabled;

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

    public void setLoginDisabled(final boolean loginDisabled) {
        this.loginDisabled = loginDisabled;
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

    @Override
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    protected User clone() {
        return new User(this);
    }
}
