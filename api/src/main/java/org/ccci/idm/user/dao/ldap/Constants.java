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
    public static final String LDAP_ATTR_THEKEY_GUID = "thekeyGuid";
    public static final String LDAP_ATTR_PASSWORD = "userPassword";
    public static final String LDAP_ATTR_FIRSTNAME = "givenName";
    public static final String LDAP_ATTR_LASTNAME = "sn";
    public static final String LDAP_ATTR_LOGINTIME = "loginTime";
    public static final String LDAP_ATTR_FACEBOOKID = "thekeyFacebookId";
    public static final String LDAP_ATTR_FACEBOOKIDSTRENGTH = "thekeyFacebookIdStrength";
    public static final String LDAP_ATTR_DOMAINSVISITED = "thekeyDomainVisited";
    public static final String LDAP_ATTR_GROUPS = "groupMembership";

    public static final String LDAP_ATTR_RESETPASSWORDKEY = "thekeyResetPasswordKey";
    public static final String LDAP_ATTR_SIGNUPKEY = "thekeySignupKey";
    public static final String LDAP_ATTR_CHANGEEMAILKEY = "thekeyChangeEmailKey";
    public static final String LDAP_ATTR_PROPOSEDEMAIL = "thekeyProposedEmail";

    public static final String LDAP_FLAG_ALLOWPASSWORDCHANGE = "passwordAllowChange";
    public static final String LDAP_FLAG_LOGINDISABLED = "loginDisabled";
    public static final String LDAP_FLAG_LOCKED = "lockedByIntruder";
    public static final String LDAP_FLAG_STALEPASSWORD = "thekeyPasswordForceChange";
    public static final String LDAP_FLAG_EMAILVERIFIED = "thekeyAccountVerified";

    // LDAP objectClass values
    public static final String LDAP_OBJECTCLASS_TOP = "Top";
    public static final String LDAP_OBJECTCLASS_PERSON = "Person";
    public static final String LDAP_OBJECTCLASS_NDSLOGIN = "ndsLoginProperties";
    public static final String LDAP_OBJECTCLASS_ORGANIZATIONALPERSON = "organizationalPerson";
    public static final String LDAP_OBJECTCLASS_INETORGPERSON = "inetOrgPerson";
    public static final String LDAP_OBJECTCLASS_THEKEYATTRIBUTES = "thekeyAttributes";
    public static final String LDAP_OBJECTCLASS_RELAY_ATTRIBUTES = "relayAttributes";
    public static final String LDAP_OBJECTCLASS_CRU_PERSON_ATTRIBUTES = "cruPerson";
    public static final String[] LDAP_OBJECTCLASSES_USER = new String[]{LDAP_OBJECTCLASS_TOP,
            LDAP_OBJECTCLASS_PERSON, LDAP_OBJECTCLASS_NDSLOGIN, LDAP_OBJECTCLASS_ORGANIZATIONALPERSON,
            LDAP_OBJECTCLASS_INETORGPERSON, LDAP_OBJECTCLASS_THEKEYATTRIBUTES};

    // CruPerson attributes
    public static final String LDAP_ATTR_CRU_DESIGNATION = "cruDesignation";
    public static final String LDAP_ATTR_CRU_EMPLOYEE_STATUS = "cruEmployeeStatus";
    public static final String LDAP_ATTR_CRU_GENDER = "cruGender";
    public static final String LDAP_ATTR_CRU_HR_STATUS_CODE = "cruHrStatusCode";
    public static final String LDAP_ATTR_CRU_JOB_CODE = "cruJobCode";
    public static final String LDAP_ATTR_CRU_MANAGER_ID = "cruManagerID";
    public static final String LDAP_ATTR_CRU_MINISTRY_CODE = "cruMinistryCode";
    public static final String LDAP_ATTR_CRU_PAY_GROUP = "cruPayGroup";
    public static final String LDAP_ATTR_CRU_PREFERRED_NAME = "cruPreferredName";
    public static final String LDAP_ATTR_CRU_SUB_MINISTRY_CODE = "cruSubMinistryCode";
    public static final String LDAP_ATTR_CRU_PROXY_ADDRESSES = "proxyAddresses";

    // relayAttributes
    public static final String LDAP_ATTR_RELAY_GUID = "relayGuid";
    public static final String LDAP_ATTR_COUNTRY = "c";

    // other cru related attributes (available in other object classes)
    public static final String LDAP_ATTR_EMPLOYEE_NUMBER = "employeeNumber";
    public static final String LDAP_ATTR_DEPARTMENT_NUMBER = "departmentNumber";
    public static final String LDAP_ATTR_CITY = "city";
    public static final String LDAP_ATTR_STATE = "st";
    public static final String LDAP_ATTR_POSTAL_CODE = "postalCode";
    public static final String LDAP_ATTR_TELEPHONE = "telephoneNumber";
}
