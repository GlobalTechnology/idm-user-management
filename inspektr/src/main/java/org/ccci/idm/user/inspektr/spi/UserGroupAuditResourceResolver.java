package org.ccci.idm.user.inspektr.spi;

import com.github.inspektr.audit.spi.support.AbstractAuditResourceResolver;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

public class UserGroupAuditResourceResolver extends AbstractAuditResourceResolver {
    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 2 && args[0] instanceof User && args[1] instanceof Group) {
            final User user = (User) args[0];
            return new String[]{"{guid=" + user.getGuid() + ",email=" + user.getEmail() + "} to/from " + args[1]};
        }

        return null;
    }
}
