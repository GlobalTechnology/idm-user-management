package org.ccci.idm.user;

import org.ccci.idm.user.dao.CruUserDao;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class CruUserManager extends DefaultUserManager {

    @Inject
    @NotNull
    protected CruUserDao cruUserDao;

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmail(final String employeeId, final boolean includeDeactivated) {
        //TODO: implement includeDeactivated functionality
        return this.cruUserDao.findByEmployeeId(employeeId);
    }
}
