package org.ccci.idm.user.ldaptive.dao.mapper;

import com.google.common.base.Strings;
import org.ccci.idm.user.Group;
import org.ldaptive.LdapAttribute;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupDnResolver {
    private String baseDn = "";
    private String pathRdnAttr = "ou";
    private String nameRdnAttr = "cn";

    public String getBaseDn() {
        return this.baseDn;
    }

    public void setBaseDn(@Nullable final String dn) {
        this.baseDn = Strings.nullToEmpty(dn);
    }

    public String getPathRdnAttr() {
        return this.pathRdnAttr;
    }

    public void setPathRdnAttr(@Nullable final String rdnAttr) {
        this.pathRdnAttr = Strings.nullToEmpty(rdnAttr);
    }

    public String getNameRdnAttr() {
        return this.nameRdnAttr;
    }

    public void setNameRdnAttr(@Nonnull final String rdnAttr) {
        if (Strings.isNullOrEmpty(rdnAttr)) {
            throw new IllegalArgumentException("Name RDN Attribute cannot be empty");
        }

        this.nameRdnAttr = rdnAttr;
    }

    private final String delimiter = ",";
    private final Character valueDelimiter = '=';

    public String resolve(@Nonnull final Group group) {
        final StringBuilder sb = new StringBuilder();

        sb.append(this.nameRdnAttr).append(valueDelimiter).append(LdapAttribute.escapeValue(group.getName()));

        List<String> path = Arrays.asList(group.getPath());
        Collections.reverse(path);
        // append path components
        for (final String component : path) {
            if(sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(this.pathRdnAttr).append(valueDelimiter).append(LdapAttribute.escapeValue(component));
        }

        sb.append(delimiter + this.baseDn);

        // return generated DN
        return sb.toString();
    }
}
