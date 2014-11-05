package org.ccci.idm.user;

import static org.ccci.idm.user.Constants.STRENGTH_FULL;
import static org.ccci.idm.user.Constants.STRENGTH_NONE;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class User implements Cloneable, Serializable {
    private static final long serialVersionUID = -1174980195690210236L;

    public enum Attr {EMAIL, PASSWORD, NAME, LOGINTIME, FLAGS, SELFSERVICEKEYS, DOMAINSVISITED, FACEBOOK, RELAY_GUID,
       LOCATION, CRU_PERSON }

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
    private final Set<String> domainsVisited = new HashSet<String>();
    private final Set<String> groups = new HashSet<String>();

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

    // Cru person attributes
    private String employeeId;
    private String departmentNumber;
    private String cruDesignation;
    private String cruEmployeeStatus;
    private String cruGender;
    private String cruHrStatusCode;
    private String cruJobCode;
    private String cruManagerID;
    private String cruMinistryCode;
    private String cruPayGroup;
    private String cruPreferredName;
    private String cruSubMinistryCode;
    private Collection<String> cruProxyAddresses = Sets.newHashSet();

    // other attributes (used by relay)
    private String city;
    private String state;
    private String postal;
    private String country;

    private String telephoneNumber;

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

        this.employeeId = source.employeeId;
        this.departmentNumber = source.departmentNumber;
        this.cruDesignation = source.cruDesignation;
        this.cruEmployeeStatus = source.cruEmployeeStatus;
        this.cruGender = source.cruGender;
        this.cruHrStatusCode = source.cruHrStatusCode;
        this.cruJobCode = source.cruJobCode;
        this.cruManagerID = source.cruManagerID;
        this.cruMinistryCode = source.cruMinistryCode;
        this.cruPayGroup = source.cruPayGroup;
        this.cruPreferredName = source.cruPreferredName;
        this.cruSubMinistryCode = source.cruSubMinistryCode;
        this.cruProxyAddresses.addAll(source.cruProxyAddresses);
        this.country = source.country;

        this.city = source.city;
        this.state = source.state;
        this.postal = source.postal;
        this.telephoneNumber = source.telephoneNumber;
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


    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartmentNumber() {
        return departmentNumber;
    }

    public void setDepartmentNumber(String departmentNumber) {
        this.departmentNumber = departmentNumber;
    }

    public String getCruDesignation() {
        return cruDesignation;
    }

    public void setCruDesignation(String cruDesignation) {
        this.cruDesignation = cruDesignation;
    }

    public String getCruEmployeeStatus() {
        return cruEmployeeStatus;
    }

    public void setCruEmployeeStatus(String cruEmployeeStatus) {
        this.cruEmployeeStatus = cruEmployeeStatus;
    }

    public String getCruGender() {
        return cruGender;
    }

    public void setCruGender(String cruGender) {
        this.cruGender = cruGender;
    }

    public String getCruHrStatusCode() {
        return cruHrStatusCode;
    }

    public void setCruHrStatusCode(String cruHrStatusCode) {
        this.cruHrStatusCode = cruHrStatusCode;
    }

    public String getCruJobCode() {
        return cruJobCode;
    }

    public void setCruJobCode(String cruJobCode) {
        this.cruJobCode = cruJobCode;
    }

    public String getCruManagerID() {
        return cruManagerID;
    }

    public void setCruManagerID(String cruManagerID) {
        this.cruManagerID = cruManagerID;
    }

    public String getCruMinistryCode() {
        return cruMinistryCode;
    }

    public void setCruMinistryCode(String cruMinistryCode) {
        this.cruMinistryCode = cruMinistryCode;
    }

    public String getCruPayGroup() {
        return cruPayGroup;
    }

    public void setCruPayGroup(String cruPayGroup) {
        this.cruPayGroup = cruPayGroup;
    }

    public String getCruPreferredName() {
        return cruPreferredName;
    }

    public void setCruPreferredName(String cruPreferredName) {
        this.cruPreferredName = cruPreferredName;
    }

    public String getCruSubMinistryCode() {
        return cruSubMinistryCode;
    }

    public void setCruSubMinistryCode(String cruSubMinistryCode) {
        this.cruSubMinistryCode = cruSubMinistryCode;
    }

    public Collection<String> getCruProxyAddresses() {
        return cruProxyAddresses;
    }

    public void setCruProxyAddresses(Collection<String> cruProxyAddresses) {
        this.cruProxyAddresses = cruProxyAddresses;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostal() {
        return postal;
    }

    public void setPostal(String postal) {
        this.postal = postal;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    /**
     * @return the domainsVisited
     */
    public Set<String> getDomainsVisited() {
        return Collections.unmodifiableSet(this.domainsVisited);
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
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(this.groups);
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("email", email)
                .add("guid", guid)
                .add("theKeyGuid", theKeyGuid)
                .add("relayGuid", relayGuid)
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("emailVerified", emailVerified)
                .add("allowPasswordChange", allowPasswordChange)
                .add("forcePasswordChange", forcePasswordChange)
                .add("deactivated", deactivated)
                .add("loginDisabled", loginDisabled)
                .add("locked", locked)
                .add("domainsVisited", domainsVisited)
                .add("groups", groups)
                .add("signupKey", signupKey)
                .add("changeEmailKey", changeEmailKey)
                .add("resetPasswordKey", resetPasswordKey)
                .add("proposedEmail", proposedEmail)
                .add("facebookId", facebookId)
                .add("facebookIdStrength", facebookIdStrength)
                .add("deactivatedUid", deactivatedUid)
                .add("employeeId", employeeId)
                .add("departmentNumber", departmentNumber)
                .add("cruDesignation", cruDesignation)
                .add("cruEmployeeStatus", cruEmployeeStatus)
                .add("cruGender", cruGender)
                .add("cruHrStatusCode", cruHrStatusCode)
                .add("cruJobCode", cruJobCode)
                .add("cruManagerID", cruManagerID)
                .add("cruMinistryCode", cruMinistryCode)
                .add("cruPayGroup", cruPayGroup)
                .add("cruPreferredName", cruPreferredName)
                .add("cruSubMinistryCode", cruSubMinistryCode)
                .add("cruProxyAddresses", cruProxyAddresses)
                .add("city", city)
                .add("state", state)
                .add("postal", postal)
                .add("country", country)
                .add("telephoneNumber", telephoneNumber)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email, password, guid, theKeyGuid, relayGuid, firstName, lastName, emailVerified,
                allowPasswordChange, forcePasswordChange, deactivated, loginDisabled, locked, domainsVisited, groups,
                signupKey, changeEmailKey, resetPasswordKey, proposedEmail, facebookId, facebookIdStrength,
                deactivatedUid, employeeId, departmentNumber, cruDesignation, cruEmployeeStatus, cruGender,
                cruHrStatusCode, cruJobCode, cruManagerID, cruMinistryCode, cruPayGroup, cruPreferredName,
                cruSubMinistryCode, cruProxyAddresses, city, state, postal, country, telephoneNumber);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}
        final User other = (User) obj;
        return Objects.equal(this.email, other.email) &&
                Objects.equal(this.password, other.password) &&
                Objects.equal(this.guid, other.guid) &&
                Objects.equal(this.theKeyGuid, other.theKeyGuid) &&
                Objects.equal(this.relayGuid, other.relayGuid) &&
                Objects.equal(this.firstName, other.firstName) &&
                Objects.equal(this.lastName, other.lastName) &&
                Objects.equal(this.emailVerified, other.emailVerified) &&
                Objects.equal(this.allowPasswordChange, other.allowPasswordChange) &&
                Objects.equal(this.forcePasswordChange, other.forcePasswordChange) &&
                Objects.equal(this.deactivated, other.deactivated) &&
                Objects.equal(this.loginDisabled, other.loginDisabled) &&
                Objects.equal(this.locked, other.locked) &&
                this.domainsVisited.size() == other.domainsVisited.size() && this.domainsVisited.containsAll(other.domainsVisited) &&
                this.groups.size() == other.groups.size() && this.groups.containsAll(other.groups) &&
                Objects.equal(this.signupKey, other.signupKey) &&
                Objects.equal(this.changeEmailKey, other.changeEmailKey) &&
                Objects.equal(this.resetPasswordKey, other.resetPasswordKey) &&
                Objects.equal(this.proposedEmail, other.proposedEmail) &&
                Objects.equal(this.facebookId, other.facebookId) &&
                Objects.equal(this.facebookIdStrength, other.facebookIdStrength) &&
                Objects.equal(this.deactivatedUid, other.deactivatedUid) &&
                Objects.equal(this.employeeId, other.employeeId) &&
                Objects.equal(this.departmentNumber, other.departmentNumber) &&
                Objects.equal(this.cruDesignation, other.cruDesignation) &&
                Objects.equal(this.cruEmployeeStatus, other.cruEmployeeStatus) &&
                Objects.equal(this.cruGender, other.cruGender) &&
                Objects.equal(this.cruHrStatusCode, other.cruHrStatusCode) &&
                Objects.equal(this.cruJobCode, other.cruJobCode) &&
                Objects.equal(this.cruManagerID, other.cruManagerID) &&
                Objects.equal(this.cruMinistryCode, other.cruMinistryCode) &&
                Objects.equal(this.cruPayGroup, other.cruPayGroup) &&
                Objects.equal(this.cruPreferredName, other.cruPreferredName) &&
                Objects.equal(this.cruSubMinistryCode, other.cruSubMinistryCode) &&
                this.cruProxyAddresses.size() == other.cruProxyAddresses.size() && this.cruProxyAddresses.containsAll(other.cruProxyAddresses) &&
                Objects.equal(this.city, other.city) &&
                Objects.equal(this.state, other.state) &&
                Objects.equal(this.postal, other.postal) &&
                Objects.equal(this.country, other.country) &&
                Objects.equal(this.telephoneNumber, other.telephoneNumber);
    }
}
