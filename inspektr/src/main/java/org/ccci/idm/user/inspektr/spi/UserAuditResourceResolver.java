package org.ccci.idm.user.inspektr.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.ccci.idm.user.User;
import org.jasig.inspektr.audit.spi.support.AbstractAuditResourceResolver;

import javax.annotation.Nonnull;

public class UserAuditResourceResolver extends AbstractAuditResourceResolver {
    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 1 && args[0] instanceof User) {
            final User user = (User) args[0];
            return new String[]{userToString(user).toString()};
        }

        return null;
    }

    @Nonnull
    protected ToStringHelper userToString(@Nonnull final User user) {
        return MoreObjects.toStringHelper(user).add("guid", user.getGuid()).add("email", user.getEmail());
    }
}
