package org.ccci.idm.user.ldaptive.dao.io;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.ccci.idm.user.Group;
import org.ldaptive.LdapAttribute;
import org.ldaptive.io.AbstractStringValueTranscoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupValueTranscoder extends AbstractStringValueTranscoder<Group> {
    private static final String delimiter = ",";
    private static final String valueDelimiter = "=";

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

    @Override
    public Class<Group> getType() {
        return Group.class;
    }

    @Override
    public String encodeStringValue(@Nonnull final Group group) {
        final StringBuilder sb = new StringBuilder();

        sb.append(this.nameRdnAttr).append(valueDelimiter).append(LdapAttribute.escapeValue(group.getName()));

        // append path components
        for (final String component : Lists.reverse(Arrays.asList(group.getPath()))) {
            if(sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(this.pathRdnAttr).append(valueDelimiter).append(LdapAttribute.escapeValue(component));
        }

        if (baseDn.length() > 0) {
            sb.append(delimiter).append(this.baseDn);
        }

        // return generated DN
        return sb.toString();
    }

    @Override
    public Group decodeStringValue(@Nonnull final String groupDn) {
        // make sure the group DN ends with the base DN (plus delimiter) if we have a base DN
        if (baseDn.length() > 0 && !groupDn.toLowerCase().endsWith(delimiter + baseDn.toLowerCase())) {
            throw new IllegalGroupDnException(groupDn);
        }

        final String relative;
        if (baseDn.length() > 0) {
            relative = groupDn.substring(0, groupDn.length() - baseDn.length() - 1);
        } else {
            relative = groupDn;
        }

        List<String> path = Lists.newArrayList();
        String name = null;
        for(String element : relative.split(delimiter))
        {
            if(element.toLowerCase().startsWith(pathRdnAttr + valueDelimiter))
            {
                path.add(element.split(valueDelimiter)[1]);
            }
            else if(element.toLowerCase().startsWith(nameRdnAttr + valueDelimiter))
            {
                name = element.split(valueDelimiter)[1];
            }
        }

        // throw an exception if we didn't find a name component
        if (name == null) {
            throw new IllegalGroupDnException(groupDn);
        }

        Collections.reverse(path);

        return new Group(path.toArray(new String[path.size()]), name);
    }

    public static class IllegalGroupDnException extends IllegalArgumentException {
        private static final long serialVersionUID = 3119012756644385809L;

        @Nonnull
        private final String groupDn;

        public IllegalGroupDnException(@Nonnull final String groupDn) {
            super("Group " + groupDn + " cannot be parsed");
            this.groupDn = groupDn;
        }

        @Nonnull
        public String getGroupDn() {
            return groupDn;
        }
    }
}
