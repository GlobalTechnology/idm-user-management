package org.ccci.idm.user.inspektr.resolver;

import static org.ccci.idm.user.inspektr.util.InspektrSerializationUtils.userToStringHelper;

import org.aspectj.lang.JoinPoint;
import org.ccci.idm.user.User;
import org.jasig.inspektr.audit.spi.AuditResourceResolver;

public class ReturnUserAuditResourceResolver implements AuditResourceResolver {
    @Override
    public String[] resolveFrom(final JoinPoint target, final Object returnValue) {
        final User user = returnValue instanceof User ? (User) returnValue : null;
        return new String[]{userToStringHelper(user).toString()};
    }

    @Override
    public String[] resolveFrom(final JoinPoint auditableTarget, final Exception exception) {
        final String message = exception.getMessage();
        if (message != null) {
            return new String[]{message};
        }
        return new String[]{exception.toString()};
    }
}
