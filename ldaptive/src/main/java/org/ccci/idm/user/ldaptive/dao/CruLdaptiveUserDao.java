package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.User;
import org.ccci.idm.user.ldaptive.dao.filter.EqualsFilter;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMPLOYEE_NUMBER;

public class CruLdaptiveUserDao extends LdaptiveUserDao {

    public User findByEmployeeId(final String employeeId) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_EMPLOYEE_NUMBER, employeeId).and(FILTER_DEACTIVATED.not()).and
                (FILTER_PERSON));
    }
}
