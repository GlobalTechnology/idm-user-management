package org.ccci.idm.user.spring.ldap.dao.mapper;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CHANGEEMAILKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_DOMAINSVISITED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKIDSTRENGTH;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GROUPS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PASSWORD;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PROPOSEDEMAIL;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RELAY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RESETPASSWORDKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_SIGNUPKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_DEACTIVATED_PREFIX;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_ALLOWPASSWORDCHANGE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_EMAILVERIFIED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_LOCKED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_LOGINDISABLED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_STALEPASSWORD;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASSES_USER;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ccci.idm.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAttributesMapper implements AttributesMapper {
    private static final Logger LOG = LoggerFactory.getLogger(UserAttributesMapper.class);

    private static final Function<Object, String> FUNCTION_CAST_STRINGS = new Function<Object, String>() {
        @Override
        public String apply(final Object input) {
            return input instanceof String ? (String) input : null;
        }
    };
    private static final Joiner JOINER_STRENGTH = Joiner.on("$").useForNull("");

    @Override
    public Object mapFromAttributes(final Attributes attrs) throws NamingException {
        final User user = new User();

        // set email & deactivated flag
        final String cn = this.getStringValue(attrs, LDAP_ATTR_CN);
        if (!cn.startsWith(LDAP_DEACTIVATED_PREFIX) && cn.contains("@")) {
            user.setEmail(cn);
            user.setDeactivated(false);
            user.setDeactivatedUid(null);
        } else {
            final String email = this.getStringValue(attrs, LDAP_ATTR_USERID);
            user.setEmail(email);
            user.setDeactivated(true);
            user.setDeactivatedUid(cn);
        }

        // Base attributes
        user.setGuid(this.getStringValue(attrs, LDAP_ATTR_GUID));
        user.setRelayGuid(this.getStringValue(attrs, LDAP_ATTR_RELAY_GUID));
        user.setTheKeyGuid(this.getStringValue(attrs, LDAP_ATTR_THEKEY_GUID));
        user.setFirstName(this.getStringValue(attrs, LDAP_ATTR_FIRSTNAME));
        user.setLastName(this.getStringValue(attrs, LDAP_ATTR_LASTNAME));

//        // Meta-data
//        user.setLoginTime(this.getTimeValue(attrs, LDAP_ATTR_LOGINTIME));
//
        // federated identities
        final Map<String, Double> facebookIdStrengths = this.getStrengthValues(attrs, LDAP_ATTR_FACEBOOKIDSTRENGTH);
        for (final String facebookId : this.getStringValues(attrs, LDAP_ATTR_FACEBOOKID)) {
            user.setFacebookId(facebookId, facebookIdStrengths.get(facebookId));
        }

        // Multi-value attributes
        user.setGroups(this.getStringValues(attrs, LDAP_ATTR_GROUPS));
        user.setDomainsVisited(this.getStringValues(attrs, LDAP_ATTR_DOMAINSVISITED));

        // Flags
        user.setAllowPasswordChange(this.getBooleanValue(attrs, LDAP_FLAG_ALLOWPASSWORDCHANGE, true));
        user.setLoginDisabled(this.getBooleanValue(attrs, LDAP_FLAG_LOGINDISABLED, false));
        user.setLocked(this.getBooleanValue(attrs, LDAP_FLAG_LOCKED, false));
        user.setForcePasswordChange(this.getBooleanValue(attrs, LDAP_FLAG_STALEPASSWORD, false));
        user.setEmailVerified(this.getBooleanValue(attrs, LDAP_FLAG_EMAILVERIFIED, false));


        // various self-service keys
        user.setSignupKey(this.getStringValue(attrs, LDAP_ATTR_SIGNUPKEY));
        user.setChangeEmailKey(this.getStringValue(attrs, LDAP_ATTR_CHANGEEMAILKEY));
        user.setProposedEmail(this.getStringValue(attrs, LDAP_ATTR_PROPOSEDEMAIL));
        user.setResetPasswordKey(this.getStringValue(attrs, LDAP_ATTR_RESETPASSWORDKEY));

        // return the loaded User object
        LOG.debug("User loaded from LDAP: {}", user.getGuid());
        return user;
    }

    public Attributes mapToAttributes(final User user) {
        // build the Attributes object
        final Attributes attrs = new BasicAttributes(true);

        // set the object class for this GcxUser
        final Attribute objectClass = new BasicAttribute(LDAP_ATTR_OBJECTCLASS);
        for (final String clazz : LDAP_OBJECTCLASSES_USER) {
            objectClass.add(clazz);
        }
        attrs.put(objectClass);

        // set the email for this user
        attrs.put(LDAP_ATTR_CN, user.isDeactivated() ? LDAP_DEACTIVATED_PREFIX + user.getGuid() : user.getEmail());
        attrs.put(LDAP_ATTR_USERID, user.getEmail());

        // set the simple attributes for this user
        attrs.put(LDAP_ATTR_GUID, user.getGuid());
        attrs.put(LDAP_ATTR_RELAY_GUID, user.getRawRelayGuid());
        attrs.put(LDAP_ATTR_THEKEY_GUID, user.getRawTheKeyGuid());
        attrs.put(LDAP_ATTR_FIRSTNAME, user.getFirstName());
        attrs.put(LDAP_ATTR_LASTNAME, user.getLastName());

        // set several flags for this user
        attrs.put(LDAP_FLAG_ALLOWPASSWORDCHANGE, Boolean.toString(user.isAllowPasswordChange()).toUpperCase());
        attrs.put(LDAP_FLAG_LOGINDISABLED, Boolean.toString(user.isLoginDisabled()).toUpperCase());
        attrs.put(LDAP_FLAG_STALEPASSWORD, Boolean.toString(user.isForcePasswordChange()).toUpperCase());
        attrs.put(LDAP_FLAG_EMAILVERIFIED, Boolean.toString(user.isEmailVerified()).toUpperCase());

        // set the multi-valued attributes
        final Attribute domains = new BasicAttribute(LDAP_ATTR_DOMAINSVISITED);
        for (final String domain : user.getDomainsVisited()) {
            domains.add(domain);
        }
        attrs.put(domains);

        // store any self-service keys
        attrs.put(LDAP_ATTR_SIGNUPKEY, user.getSignupKey());
        attrs.put(LDAP_ATTR_CHANGEEMAILKEY, user.getChangeEmailKey());
        attrs.put(LDAP_ATTR_PROPOSEDEMAIL, user.getProposedEmail());
        attrs.put(LDAP_ATTR_RESETPASSWORDKEY, user.getResetPasswordKey());

        final String password = user.getPassword();
        if (StringUtils.hasText(password)) {
            attrs.put(LDAP_ATTR_PASSWORD, password);
        }
//        final Date loginTime = user.getLoginTime();
//        if (loginTime != null) {
//            attrs.put(LDAP_ATTR_LOGINTIME, this.convertToGeneralizedTime(loginTime));
//        }

        // set any federated identities
        final String facebookId = user.getFacebookId();
        attrs.put(LDAP_ATTR_FACEBOOKID, facebookId);
        attrs.put(LDAP_ATTR_FACEBOOKIDSTRENGTH, encodeStrength(facebookId, user.getFacebookIdStrengthFor(facebookId)));

        // return the generated attributes
        return attrs;
    }

    /**
     * Get the specified attribute value safely, in case it isn't present, and would otherwise throw a {@link
     * NullPointerException}.
     *
     * @param attrs {@link Attributes} returned from the LDAP server.
     * @param name  Name of {@link Attribute} to retrieve.
     * @return Recovered {@link Attribute} or <tt>null</tt> if not present.
     * @throws NamingException if the specified attribute name is not found
     */
    private Object getValue(final Attributes attrs, final String name) throws NamingException {
        final Attribute attr = attrs.get(name);
        if (attr != null && attr.size() > 0) {
            return attr.get();
        }

        return null;
    }

    private List<?> getValues(final Attributes attrs, final String name) throws NamingException {
        final Attribute attr = attrs.get(name);
        if (attr != null) {
            return Collections.list(attr.getAll());
        }

        return Collections.emptyList();
    }

    private String getStringValue(final Attributes attrs, final String name) throws NamingException {
        final Object value = this.getValue(attrs, name);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    private List<String> getStringValues(final Attributes attrs, final String name) throws NamingException {
        return ImmutableList.copyOf(Collections2.filter(Lists.transform(this.getValues(attrs, name),
                FUNCTION_CAST_STRINGS), Predicates.notNull()));
    }

    private boolean getBooleanValue(final Attributes attrs, final String name, final boolean defaultValue) throws
            NamingException {
        final String value = this.getStringValue(attrs, name);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    private Map<String, Double> getStrengthValues(final Attributes attrs, final String name) throws NamingException {
        final Map<String, Double> strengths = new HashMap<String, Double>();
        for (final String value : this.getStringValues(attrs, name)) {
            final String[] values = StringUtils.split(value, "$");

            // only add valid values
            if (values.length != 2) {
                continue;
            }

            // set the strength value, catching any parsing errors
            try {
                strengths.put(values[0], Double.parseDouble(values[1]));
            } catch (final Exception ignored) {
            }
        }

        return strengths;
    }

    private String encodeStrength(final String id, final Double strength) {
        return id != null ? JOINER_STRENGTH.join(id, strength) : null;
    }
}
