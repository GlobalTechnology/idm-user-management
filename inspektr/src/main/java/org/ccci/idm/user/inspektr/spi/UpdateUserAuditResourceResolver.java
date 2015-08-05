package org.ccci.idm.user.inspektr.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.ccci.idm.user.User;
import org.ccci.idm.user.User.Attr;

import javax.annotation.Nonnull;

public class UpdateUserAuditResourceResolver extends UserAuditResourceResolver {
    @Override
    protected String[] createResource(final Object[] args) {
        if (args.length >= 2 && args[0] instanceof User && args[1] instanceof Attr[]) {
            final User user = (User) args[0];
            final Attr[] attrs = (Attr[]) args[1];

            // generate Attr[] output
            final ToStringHelper attrsStr = MoreObjects.toStringHelper("Attrs");
            for (final Attr attr : (Attr[]) args[1]) {
                attrsStr.addValue(attr);
            }

            return new String[]{userToString(user, attrs) + " " + attrsStr};
        }

        return super.createResource(args);
    }

    @Nonnull
    protected ToStringHelper userToString(@Nonnull final User user, @Nonnull final Attr[] attrs) {
        final ToStringHelper output = userToString(user);

        for (final Attr attr : attrs) {
            if (attr != null) {
                switch (attr) {
                    case NAME:
                        output.add("firstName", user.getFirstName()).add("lastName", user.getLastName());
                        break;
                    case FACEBOOK:
                        output.add("facebookId", user.getFacebookId());
                        break;
                    case EMAIL:
                        // email was already added in base method, so no need to add it again
                    case PASSWORD:
                        // don't actually log the password...
                    default:
                        // do nothing for unhandled attributes
                }
            }
        }

        return output;
    }
}
