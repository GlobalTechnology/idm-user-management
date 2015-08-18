package org.ccci.idm.user.inspektr.spi;

import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

public class UserGroupAuditResourceResolver extends UserAuditResourceResolver {
    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 2 && args[0] instanceof User && args[1] instanceof Group) {
            final User user = (User) args[0];
            final Group group = (Group) args[1];
            return new String[]{userToString(user) + " " + group};
        }

        return null;
    }
}
