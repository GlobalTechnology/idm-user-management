package org.ccci.idm.user.inspektr.spi;

import com.github.inspektr.audit.spi.support.AbstractAuditResourceResolver;
import org.ccci.idm.user.User;

public class UserAuditResourceResolver extends AbstractAuditResourceResolver {
    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 1 && args[0] instanceof User) {
            final User user = (User) args[0];
            return new String[]{"{guid=" + user.getTheKeyGuid() + ",email=" + user.getEmail() + "}"};
        }
        return null;
    }
}
