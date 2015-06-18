package org.ccci.idm.user.inspektr.spi;

import com.github.inspektr.audit.spi.support.AbstractAuditResourceResolver;
import com.google.common.base.Joiner;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

public class UserGroupAuditResourceResolver extends AbstractAuditResourceResolver {
    private static final Joiner GROUP_JOINER = Joiner.on(".").skipNulls();

    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 2 && args[0] instanceof User && args[1] instanceof Group) {
            final User user = (User) args[0];
            final Group group = (Group) args[1];
            return new String[]{"User: {guid=" + user.getTheKeyGuid() + ", email=" + user.getEmail() + "} " +
                    "Group: {path=" + GROUP_JOINER.join(group.getPath()) + ", name=" + group.getName() + "}"};
        }

        return null;
    }
}
