package org.ccci.idm.user.query;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_DESIGNATION;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_PROXY_ADDRESSES;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMPLOYEE_NUMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GROUPS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;

import org.ccci.idm.user.Group;

import javax.annotation.Nonnull;

public enum Attribute {
    GUID(LDAP_ATTR_THEKEY_GUID),
    EMAIL(LDAP_ATTR_USERID),
    EMAIL_ALIAS(LDAP_ATTR_CRU_PROXY_ADDRESSES),
    FIRST_NAME(LDAP_ATTR_FIRSTNAME),
    LAST_NAME(LDAP_ATTR_LASTNAME),
    US_EMPLOYEE_ID(LDAP_ATTR_EMPLOYEE_NUMBER),
    US_DESIGNATION(LDAP_ATTR_CRU_DESIGNATION),
    GROUP(LDAP_ATTR_GROUPS);

    public final String ldapAttr;

    Attribute(final String ldapAttr) {
        this.ldapAttr = ldapAttr;
    }

    public Expression eq(@Nonnull final String value) {
        return new ComparisonExpression(ComparisonExpression.Type.EQ, this, value);
    }

    public Expression eq(@Nonnull final Group group) {
        if (this != GROUP) {
            throw new UnsupportedOperationException("You can only test group equality for the GROUP attribute");
        }
        return new ComparisonExpression(ComparisonExpression.Type.EQ, this, group);
    }

    public Expression like(@Nonnull final String value) {
        if (this == GUID) {
            throw new UnsupportedOperationException("You can only do direct equality searches for guids");
        }

        return new ComparisonExpression(ComparisonExpression.Type.LIKE, this, value);
    }

    public Expression sw(@Nonnull final String value) {
        if (this == GUID) {
            throw new UnsupportedOperationException("You can only do direct equality searches for guids");
        }

        return new ComparisonExpression(ComparisonExpression.Type.SW, this, value);
    }
}
