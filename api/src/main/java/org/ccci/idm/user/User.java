package org.ccci.idm.user;

import java.io.Serializable;

public class User implements Cloneable, Serializable {
    private String email;
    private String password;

    private String guid;
    private String theKeyGuid;
    private String relayGuid;

    // account flags
    private boolean emailVerified = false;
    private boolean forcePasswordChange = false;

    public User() {
    }

    protected User(User source) {
        this.email = source.email;
        this.password = source.password;
        this.guid = source.guid;
        this.theKeyGuid = source.theKeyGuid;
        this.relayGuid = source.relayGuid;
        this.emailVerified = source.emailVerified;
        this.forcePasswordChange = source.forcePasswordChange;
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

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(final boolean force) {
        this.forcePasswordChange = force;
    }

    @Override
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    protected User clone() {
        return new User(this);
    }
}
