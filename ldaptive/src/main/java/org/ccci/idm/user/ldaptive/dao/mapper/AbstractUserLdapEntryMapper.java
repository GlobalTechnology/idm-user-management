package org.ccci.idm.user.ldaptive.dao.mapper;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CHANGEEMAILKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_DOMAINSVISITED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMAIL;
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
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASSES_KEYUSER;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.ccci.idm.user.User;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.beans.LdapEntryMapper;
import org.ldaptive.io.BooleanValueTranscoder;
import org.ldaptive.io.ValueTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUserLdapEntryMapper<O extends User> implements LdapEntryMapper<O> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUserLdapEntryMapper.class);

    private static final Joiner JOINER_STRENGTH = Joiner.on("$").useForNull("");

    private static final ValueTranscoder<Boolean> TRANSCODER_BOOLEAN = new BooleanValueTranscoder(true);

    @NotNull
    protected DnResolver dnResolver;

    public void setDnResolver(final DnResolver dnResolver) {
        this.dnResolver = dnResolver;
    }

    @Override
    public String mapDn(final O user) {
        try {
            final String uid;
            if (!user.isDeactivated()) {
                uid = user.getEmail();
            } else if (user.getDeactivatedUid() != null) {
                uid = user.getDeactivatedUid();
            } else {
                uid = LDAP_DEACTIVATED_PREFIX + user.getGuid();
                user.setDeactivatedUid(uid);
            }
            return this.dnResolver.resolve(uid);
        } catch (final LdapException e) {
            LOG.error("unexpected exception generating DN", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void map(final O user, final LdapEntry entry) {
        // populate non-modifiable LdapAttributes
        entry.addAttribute(new LdapAttribute(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASSES_KEYUSER));

        // set the email for this user
        entry.addAttribute(this.attr(LDAP_ATTR_EMAIL, user.isDeactivated() ? LDAP_DEACTIVATED_PREFIX + user.getGuid()
                : user.getEmail()));
        entry.addAttribute(this.attr(LDAP_ATTR_USERID, user.getEmail()));

        // set the simple attributes for this user
        entry.addAttribute(this.attr(LDAP_ATTR_GUID, user.getGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_RELAY_GUID, user.getRawRelayGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_THEKEY_GUID, user.getRawTheKeyGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_FIRSTNAME, user.getFirstName()));
        entry.addAttribute(this.attr(LDAP_ATTR_LASTNAME, user.getLastName()));

        // set several flags for this user
        entry.addAttribute(this.attr(LDAP_FLAG_ALLOWPASSWORDCHANGE, user.isAllowPasswordChange()));
        entry.addAttribute(this.attr(LDAP_FLAG_LOGINDISABLED, user.isLoginDisabled()));
        entry.addAttribute(this.attr(LDAP_FLAG_STALEPASSWORD, user.isForcePasswordChange()));
        entry.addAttribute(this.attr(LDAP_FLAG_EMAILVERIFIED, user.isEmailVerified()));

        // set the multi-valued attributes
        entry.addAttribute(this.attr(LDAP_ATTR_DOMAINSVISITED, user.getDomainsVisited()));

        // store any self-service keys
        entry.addAttribute(this.attr(LDAP_ATTR_SIGNUPKEY, user.getSignupKey()));
        entry.addAttribute(this.attr(LDAP_ATTR_CHANGEEMAILKEY, user.getChangeEmailKey()));
        entry.addAttribute(this.attr(LDAP_ATTR_PROPOSEDEMAIL, user.getProposedEmail()));
        entry.addAttribute(this.attr(LDAP_ATTR_RESETPASSWORDKEY, user.getResetPasswordKey()));

        final String password = user.getPassword();
        if (StringUtils.hasText(password)) {
            entry.addAttribute(this.attr(LDAP_ATTR_PASSWORD, password));
        }
//        final Date loginTime = user.getLoginTime();
//        if (loginTime != null) {
//            entry.addAttribute(this.attr(LDAP_ATTR_LOGINTIME, loginTime));
//        }

        // set any federated identities
        final String facebookId = user.getFacebookId();
        entry.addAttribute(this.attr(LDAP_ATTR_FACEBOOKID, facebookId));
        entry.addAttribute(this.attr(LDAP_ATTR_FACEBOOKIDSTRENGTH, encodeStrength(facebookId,
                user.getFacebookIdStrengthFor(facebookId))));
    }

    @Override
    public void map(final LdapEntry entry, final O user) {
        // set email & deactivated flag
        final String cn = this.getStringValue(entry, LDAP_ATTR_CN);
        if (!cn.startsWith(LDAP_DEACTIVATED_PREFIX) && cn.contains("@")) {
            user.setEmail(cn);
            user.setDeactivated(false);
            user.setDeactivatedUid(null);
        } else {
            final String email = this.getStringValue(entry, LDAP_ATTR_USERID);
            user.setEmail(email);
            user.setDeactivated(true);
            user.setDeactivatedUid(cn);
        }

        // Base attributes
        user.setGuid(this.getStringValue(entry, LDAP_ATTR_GUID));
        user.setRelayGuid(this.getStringValue(entry, LDAP_ATTR_RELAY_GUID));
        user.setTheKeyGuid(this.getStringValue(entry, LDAP_ATTR_THEKEY_GUID));
        user.setFirstName(this.getStringValue(entry, LDAP_ATTR_FIRSTNAME));
        user.setLastName(this.getStringValue(entry, LDAP_ATTR_LASTNAME));

        // Meta-data
//        user.setLoginTime(this.getTimeValue(entry, LDAP_ATTR_LOGINTIME));

        // federated identities
        final Map<String, Double> facebookIdStrengths = this.getStrengthValues(entry, LDAP_ATTR_FACEBOOKIDSTRENGTH);
        for (final String facebookId : this.getStringValues(entry, LDAP_ATTR_FACEBOOKID)) {
            user.setFacebookId(facebookId, facebookIdStrengths.get(facebookId));
        }

        // Multi-value attributes
        user.setGroups(this.getStringValues(entry, LDAP_ATTR_GROUPS));
        user.setDomainsVisited(this.getStringValues(entry, LDAP_ATTR_DOMAINSVISITED));

        // Flags
        user.setAllowPasswordChange(this.getBooleanValue(entry, LDAP_FLAG_ALLOWPASSWORDCHANGE, true));
        user.setLoginDisabled(this.getBooleanValue(entry, LDAP_FLAG_LOGINDISABLED, false));
        user.setLocked(this.getBooleanValue(entry, LDAP_FLAG_LOCKED, false));
        user.setForcePasswordChange(this.getBooleanValue(entry, LDAP_FLAG_STALEPASSWORD, false));
        user.setEmailVerified(this.getBooleanValue(entry, LDAP_FLAG_EMAILVERIFIED, false));

        // various self-service keys
        user.setSignupKey(this.getStringValue(entry, LDAP_ATTR_SIGNUPKEY));
        user.setChangeEmailKey(this.getStringValue(entry, LDAP_ATTR_CHANGEEMAILKEY));
        user.setProposedEmail(this.getStringValue(entry, LDAP_ATTR_PROPOSEDEMAIL));
        user.setResetPasswordKey(this.getStringValue(entry, LDAP_ATTR_RESETPASSWORDKEY));

        // return the loaded User object
        LOG.debug("User loaded from LdapEntry: {}", user.getGuid());
    }

    protected final LdapAttribute attr(final String name, final String... values) {
        final LdapAttribute attribute = new LdapAttribute(name);
        for (final String value : values) {
            if (value != null) {
                attribute.addStringValue(value);
            }
        }
        return attribute;
    }

    protected final LdapAttribute attr(final String name, final Collection<? extends String> values) {
        return attr(name, values.toArray(new String[values.size()]));
    }

    protected final LdapAttribute attr(final String name, final boolean... values) {
        final LdapAttribute attr = new LdapAttribute(name);
        for (final boolean value : values) {
            attr.addStringValue(Boolean.toString(value).toUpperCase());
        }
        return attr;
    }

    protected final String encodeStrength(final String id, final Double strength) {
        return id != null ? JOINER_STRENGTH.join(id, strength) : null;
    }

    protected final String getStringValue(final LdapEntry entry, final String attribute) {
        final LdapAttribute attr = entry.getAttribute(attribute);
        if (attr != null) {
            return attr.getStringValue();
        }
        return null;
    }

    protected final Collection<String> getStringValues(final LdapEntry entry, final String attribute) {
        final LdapAttribute attr = entry.getAttribute(attribute);
        if (attr != null) {
            return attr.getStringValues();
        }
        return Collections.emptyList();
    }

    protected final boolean getBooleanValue(final LdapEntry entry, final String attribute) {
        return this.getBooleanValue(entry, attribute, false);
    }

    protected final boolean getBooleanValue(final LdapEntry entry, final String attribute, final boolean defaultValue) {
        final LdapAttribute attr = entry.getAttribute(attribute);
        if (attr != null) {
            final Boolean value = attr.getValue(TRANSCODER_BOOLEAN);
            if (value != null) {
                return value;
            }
        }

        return defaultValue;
    }

    protected final Map<String, Double> getStrengthValues(final LdapEntry entry, final String name) {
        final Map<String, Double> strengths = new HashMap<String, Double>();
        for (final String value : this.getStringValues(entry, name)) {
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
}
