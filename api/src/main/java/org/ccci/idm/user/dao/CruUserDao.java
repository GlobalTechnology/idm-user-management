package org.ccci.idm.user.dao;

import org.ccci.idm.user.User;

public interface CruUserDao extends UserDao
{
    /**
     * Find the user with the specified employee id.
     *
     * @param employeeId Employee id for lookup.
     * @return Requested {@link org.ccci.idm.user.User} or <tt>null</tt> if not found.
     */
    User findByEmployeeId(String employeeId);
}
