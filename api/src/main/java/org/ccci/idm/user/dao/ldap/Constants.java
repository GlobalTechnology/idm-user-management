package org.ccci.idm.user.dao.ldap;

public class Constants {
    // LDAP Deactivated
    public static final String LDAP_DEACTIVATED_PREFIX = "$GUID$-=";

    // LDAP Attributes
    public static final String LDAP_ATTR_OBJECTCLASS = "objectClass";
    public static final String LDAP_ATTR_CN = "cn";
    public static final String LDAP_ATTR_EMAIL = "cn";
    public static final String LDAP_ATTR_USERID = "uid";
    public static final String LDAP_ATTR_GUID = "extensionAttribute1";
    public static final String LDAP_ATTR_RELAY_GUID = null;
    public static final String LDAP_ATTR_THEKEY_GUID = "extensionAttribute1";
    public static final String LDAP_ATTR_PASSWORD = "userPassword";
    public static final String LDAP_ATTR_FIRSTNAME = "givenName";
    public static final String LDAP_ATTR_LASTNAME = "sn";
    public static final String LDAP_ATTR_LOGINTIME = "loginTime";
    public static final String LDAP_ATTR_FACEBOOKID = "thekeyFacebookId";
    public static final String LDAP_ATTR_FACEBOOKIDSTRENGTH = "thekeyFacebookIdStrength";
    public static final String LDAP_ATTR_DOMAINSVISITED = "extensionAttribute2";
    public static final String LDAP_ATTR_GROUPS = "groupMembership";

    public static final String LDAP_ATTR_RESETPASSWORDKEY = "thekeyResetPasswordKey";
    public static final String LDAP_ATTR_SIGNUPKEY = "thekeySignupKey";
    public static final String LDAP_ATTR_CHANGEEMAILKEY = "thekeyChangeEmailKey";
    public static final String LDAP_ATTR_PROPOSEDEMAIL = "thekeyProposedEmail";

    public static final String LDAP_FLAG_ALLOWPASSWORDCHANGE = "passwordAllowChange";
    public static final String LDAP_FLAG_LOGINDISABLED = "loginDisabled";
    public static final String LDAP_FLAG_LOCKED = "lockedByIntruder";
    public static final String LDAP_FLAG_STALEPASSWORD = "extensionAttribute5";
    public static final String LDAP_FLAG_EMAILVERIFIED = "thekeyAccountVerified";

    // LDAP objectClass values
    public static final String LDAP_OBJECTCLASS_TOP = "Top";
    public static final String LDAP_OBJECTCLASS_PERSON = "Person";
    public static final String LDAP_OBJECTCLASS_NDSLOGIN = "ndsLoginProperties";
    public static final String LDAP_OBJECTCLASS_ORGANIZATIONALPERSON = "organizationalPerson";
    public static final String LDAP_OBJECTCLASS_INETORGPERSON = "inetOrgPerson";
    public static final String LDAP_OBJECTCLASS_THEKEYATTRIBUTES = "thekeyAttributes";
    public static final String[] LDAP_OBJECTCLASSES_KEYUSER = new String[]{LDAP_OBJECTCLASS_TOP,
            LDAP_OBJECTCLASS_PERSON, LDAP_OBJECTCLASS_NDSLOGIN, LDAP_OBJECTCLASS_ORGANIZATIONALPERSON,
            LDAP_OBJECTCLASS_INETORGPERSON, LDAP_OBJECTCLASS_THEKEYATTRIBUTES};
}
