package org.ccci.idm.user.ldaptive.dao.mapper;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CHANGEEMAILKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CITY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_COUNTRY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_DESIGNATION;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_EMPLOYEE_STATUS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_GENDER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_HR_STATUS_CODE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_JOB_CODE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_MANAGER_ID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_MINISTRY_CODE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_PAY_GROUP;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_PREFERRED_NAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_PROXY_ADDRESSES;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_PASSWORD_HISTORY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_SUB_MINISTRY_CODE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_DEPARTMENT_NUMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_DOMAINSVISITED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMPLOYEE_NUMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKIDSTRENGTH;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GROUPS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LOGINTIME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PASSWORD;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PASSWORDCHANGEDTIME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_POSTAL_CODE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PROPOSEDEMAIL;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RELAY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RESETPASSWORDKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_SECURITY_ANSWER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_SECURITY_QUESTION;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_SIGNUPKEY;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_STATE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_TELEPHONE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_DEACTIVATED_PREFIX;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_ALLOWPASSWORDCHANGE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_EMAILVERIFIED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_FORCEPASSWORDCHANGE;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_LOCKED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_FLAG_LOGINDISABLED;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASSES_USER;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;
import org.ccci.idm.user.ldaptive.dao.io.ReadableInstantValueTranscoder;
import org.joda.time.ReadableInstant;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractUserLdapEntryMapper<O extends User> implements LdapEntryMapper<O> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUserLdapEntryMapper.class);

    private static final String META_DEACTIVATED_UID = "LDAPTIVE_DEACTIVATED_UID";
    private static final String META_OBJECT_CLASSES = "LDAPTIVE_OBJECT_CLASSES";

    private static final Joiner JOINER_STRENGTH = Joiner.on("$").useForNull("");

    private static final ValueTranscoder<Boolean> TRANSCODER_BOOLEAN = new BooleanValueTranscoder(true);
    private static final ValueTranscoder<ReadableInstant> TRANSCODER_INSTANT = new ReadableInstantValueTranscoder();

    @NotNull
    protected DnResolver dnResolver;

    public void setDnResolver(final DnResolver dnResolver) {
        this.dnResolver = dnResolver;
    }

    @Nullable
    protected ValueTranscoder<Group> groupValueTranscoder;

    public void setGroupValueTranscoder(@Nullable final ValueTranscoder<Group> transcoder) {
        this.groupValueTranscoder = transcoder;
    }

    @Override
    public String mapDn(final O user) {
        try {
            final String uid;
            if (!user.isDeactivated()) {
                uid = user.getEmail();
            } else if (user.getImplMeta(META_DEACTIVATED_UID, String.class) != null) {
                uid = user.getImplMeta(META_DEACTIVATED_UID, String.class);
            } else {
                uid = LDAP_DEACTIVATED_PREFIX + user.getGuid();
                user.setImplMeta(META_DEACTIVATED_UID, uid);
            }
            return this.dnResolver.resolve(uid);
        } catch (final LdapException e) {
            LOG.error("unexpected exception generating DN", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void map(@Nonnull final O user, final LdapEntry entry) {
        // populate non-modifiable LdapAttributes
        entry.addAttribute(this.attrObjectClass(user));

        // set the email for this user
        entry.addAttribute(this.attr(LDAP_ATTR_USERID, user.getEmail()));

        // set the simple attributes for this user
        entry.addAttribute(this.attr(LDAP_ATTR_GUID, user.getGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_RELAY_GUID, user.getRelayGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_THEKEY_GUID, user.getTheKeyGuid()));
        entry.addAttribute(this.attr(LDAP_ATTR_FIRSTNAME, user.getFirstName()));
        entry.addAttribute(this.attr(LDAP_ATTR_LASTNAME, user.getLastName()));

        // set several flags for this user
        entry.addAttribute(this.attr(LDAP_FLAG_ALLOWPASSWORDCHANGE, user.isAllowPasswordChange()));
        entry.addAttribute(this.attr(LDAP_FLAG_LOGINDISABLED, user.isLoginDisabled()));
        entry.addAttribute(this.attr(LDAP_FLAG_FORCEPASSWORDCHANGE, user.isForcePasswordChange()));
        entry.addAttribute(this.attr(LDAP_FLAG_EMAILVERIFIED, user.isEmailVerified()));
        // the lockedByIntruder flag is handled differently to maintain consistency with how LDAP updates the attribute
        entry.addAttribute(user.isLocked() ? this.attr(LDAP_FLAG_LOCKED, true) : this.attr(LDAP_FLAG_LOCKED));

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
        final ReadableInstant loginTime = user.getLoginTime();
        if (loginTime != null) {
            entry.addAttribute(this.attr(LDAP_ATTR_LOGINTIME, loginTime));
        }

        // set any federated identities
        final String facebookId = user.getFacebookId();
        entry.addAttribute(this.attr(LDAP_ATTR_FACEBOOKID, facebookId));
        entry.addAttribute(this.attr(LDAP_ATTR_FACEBOOKIDSTRENGTH, encodeStrength(facebookId,
                user.getFacebookIdStrengthFor(facebookId))));

        // cru person attributes
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_DESIGNATION, user.getCruDesignation()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_EMPLOYEE_STATUS, user.getCruEmployeeStatus()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_GENDER, user.getCruGender()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_HR_STATUS_CODE, user.getCruHrStatusCode()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_JOB_CODE, user.getCruJobCode()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_MANAGER_ID, user.getCruManagerID()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_MINISTRY_CODE, user.getCruMinistryCode()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_PAY_GROUP, user.getCruPayGroup()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_PREFERRED_NAME, user.getCruPreferredName()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_SUB_MINISTRY_CODE, user.getCruSubMinistryCode()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_PROXY_ADDRESSES, user.getCruProxyAddresses()));
        entry.addAttribute(this.attr(LDAP_ATTR_CRU_PASSWORD_HISTORY, user.getCruPasswordHistory()));

        entry.addAttribute(this.attr(LDAP_ATTR_EMPLOYEE_NUMBER, user.getEmployeeId()));
        entry.addAttribute(this.attr(LDAP_ATTR_DEPARTMENT_NUMBER, user.getDepartmentNumber()));
        entry.addAttribute(this.attr(LDAP_ATTR_TELEPHONE, user.getTelephoneNumber()));

        entry.addAttribute(this.attr(LDAP_ATTR_CITY, user.getCity()));
        entry.addAttribute(this.attr(LDAP_ATTR_STATE, user.getState()));
        entry.addAttribute(this.attr(LDAP_ATTR_POSTAL_CODE, user.getPostal()));
        entry.addAttribute(this.attr(LDAP_ATTR_COUNTRY, user.getCountry()));

        entry.addAttribute(this.attr(LDAP_ATTR_SECURITY_QUESTION, user.getSecurityQuestion()));
        entry.addAttribute(this.attr(LDAP_ATTR_SECURITY_ANSWER, user.getSecurityAnswer()));
    }

    @Override
    public void map(final LdapEntry entry, final O user) {
        // set email & deactivated flag accordingly
        final String cn = this.getStringValue(entry, LDAP_ATTR_CN);
        if (Strings.isNullOrEmpty(cn) || cn.startsWith(LDAP_DEACTIVATED_PREFIX)) {
            final String email = this.getStringValue(entry, LDAP_ATTR_USERID);
            user.setEmail(email);
            user.setDeactivated(true);
            user.setImplMeta(META_DEACTIVATED_UID, cn);
        } else {
            user.setEmail(cn);
            user.setDeactivated(false);
            user.removeImplMeta(META_DEACTIVATED_UID);
        }

        // capture meta-data that needs to be tracked
        user.setImplMeta(META_OBJECT_CLASSES, Sets.newHashSet(getStringValues(entry, LDAP_ATTR_OBJECTCLASS)));

        // Base attributes
        user.setGuid(this.getStringValue(entry, LDAP_ATTR_GUID));
        user.setRelayGuid(this.getStringValue(entry, LDAP_ATTR_RELAY_GUID));
        user.setTheKeyGuid(this.getStringValue(entry, LDAP_ATTR_THEKEY_GUID));
        user.setFirstName(this.getStringValue(entry, LDAP_ATTR_FIRSTNAME));
        user.setLastName(this.getStringValue(entry, LDAP_ATTR_LASTNAME));

        // Meta-data
        user.setLoginTime(this.getTimeValue(entry, LDAP_ATTR_LOGINTIME));
        user.setPasswordChangedTime(this.getTimeValue(entry, LDAP_ATTR_PASSWORDCHANGEDTIME));

        // federated identities
        final Map<String, Double> facebookIdStrengths = this.getStrengthValues(entry, LDAP_ATTR_FACEBOOKIDSTRENGTH);
        for (final String facebookId : this.getStringValues(entry, LDAP_ATTR_FACEBOOKID)) {
            user.setFacebookId(facebookId, facebookIdStrengths.get(facebookId));
        }

        // Multi-value attributes
        user.setGroups(this.getGroupValues(entry, LDAP_ATTR_GROUPS));
        user.setDomainsVisited(this.getStringValues(entry, LDAP_ATTR_DOMAINSVISITED));

        // Flags
        user.setAllowPasswordChange(this.getBooleanValue(entry, LDAP_FLAG_ALLOWPASSWORDCHANGE, true));
        user.setLoginDisabled(this.getBooleanValue(entry, LDAP_FLAG_LOGINDISABLED, false));
        user.setLocked(this.getBooleanValue(entry, LDAP_FLAG_LOCKED, false));
        user.setForcePasswordChange(this.getBooleanValue(entry, LDAP_FLAG_FORCEPASSWORDCHANGE, false));
        user.setEmailVerified(this.getBooleanValue(entry, LDAP_FLAG_EMAILVERIFIED, false));

        // various self-service keys
        user.setSignupKey(this.getStringValue(entry, LDAP_ATTR_SIGNUPKEY));
        user.setChangeEmailKey(this.getStringValue(entry, LDAP_ATTR_CHANGEEMAILKEY));
        user.setProposedEmail(this.getStringValue(entry, LDAP_ATTR_PROPOSEDEMAIL));
        user.setResetPasswordKey(this.getStringValue(entry, LDAP_ATTR_RESETPASSWORDKEY));

        // cru person attributes
        user.setCruDesignation(this.getStringValue(entry, LDAP_ATTR_CRU_DESIGNATION));
        user.setCruEmployeeStatus(this.getStringValue(entry, LDAP_ATTR_CRU_EMPLOYEE_STATUS));
        user.setCruGender(this.getStringValue(entry, LDAP_ATTR_CRU_GENDER));
        user.setCruHrStatusCode(this.getStringValue(entry, LDAP_ATTR_CRU_HR_STATUS_CODE));
        user.setCruJobCode(this.getStringValue(entry, LDAP_ATTR_CRU_JOB_CODE));
        user.setCruManagerID(this.getStringValue(entry, LDAP_ATTR_CRU_MANAGER_ID));
        user.setCruMinistryCode(this.getStringValue(entry, LDAP_ATTR_CRU_MINISTRY_CODE));
        user.setCruPayGroup(this.getStringValue(entry, LDAP_ATTR_CRU_PAY_GROUP));
        user.setCruPreferredName(this.getStringValue(entry, LDAP_ATTR_CRU_PREFERRED_NAME));
        user.setCruSubMinistryCode(this.getStringValue(entry, LDAP_ATTR_CRU_SUB_MINISTRY_CODE));
        user.setCruProxyAddresses(this.getStringValues(entry, LDAP_ATTR_CRU_PROXY_ADDRESSES));
        user.setCruPasswordHistory(this.getStringValues(entry, LDAP_ATTR_CRU_PASSWORD_HISTORY));

        user.setEmployeeId(this.getStringValue(entry, LDAP_ATTR_EMPLOYEE_NUMBER));
        user.setDepartmentNumber(this.getStringValue(entry, LDAP_ATTR_DEPARTMENT_NUMBER));
        user.setTelephoneNumber(this.getStringValue(entry, LDAP_ATTR_TELEPHONE));

        user.setCity(this.getStringValue(entry, LDAP_ATTR_CITY));
        user.setState(this.getStringValue(entry, LDAP_ATTR_STATE));
        user.setPostal(this.getStringValue(entry, LDAP_ATTR_POSTAL_CODE));
        user.setCountry(this.getStringValue(entry, LDAP_ATTR_COUNTRY));

        user.setSecurityQuestion(this.getStringValue(entry, LDAP_ATTR_SECURITY_QUESTION));
        user.setSecurityAnswer(this.getStringValue(entry, LDAP_ATTR_SECURITY_ANSWER), false);

        // return the loaded User object
        LOG.debug("User loaded from LdapEntry: {}", user.getGuid());
    }

    protected final LdapAttribute attr(@Nonnull final String name) {
        return new LdapAttribute(name);
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

    protected final LdapAttribute attr(final String name, final boolean value) {
        final LdapAttribute attr = new LdapAttribute(name);
        attr.addValue(TRANSCODER_BOOLEAN, value);
        return attr;
    }

    protected final LdapAttribute attr(final String name, final ReadableInstant... values) {
        final LdapAttribute attr = new LdapAttribute(name);
        attr.addValue(TRANSCODER_INSTANT, values);
        return attr;
    }

    protected LdapAttribute attrObjectClass(final O user) {
        // get any existing object classes
        @SuppressWarnings("unchecked")
        HashSet<String> objectClasses = (HashSet<String>) user.getImplMeta(META_OBJECT_CLASSES, HashSet.class);
        if (objectClasses == null) {
            objectClasses = Sets.newHashSet();
        }

        // update objectClasses
        objectClasses.addAll(LDAP_OBJECTCLASSES_USER);

        return this.attr(LDAP_ATTR_OBJECTCLASS, objectClasses);
    }

    protected final String encodeStrength(final String id, final Double strength) {
        return id != null ? JOINER_STRENGTH.join(id, strength) : null;
    }

    @Nullable
    protected final String getStringValue(final LdapEntry entry, final String attribute) {
        final LdapAttribute attr = entry.getAttribute(attribute);
        if (attr != null) {
            return attr.getStringValue();
        }
        return null;
    }

    @Nonnull
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

    protected final Collection<Group> getGroupValues(final LdapEntry entry, final String attribute) {
        final Collection<Group> groups = Sets.newHashSet();

        if (this.groupValueTranscoder != null) {
            for (final String dn : this.getStringValues(entry, attribute)) {
                try {
                    groups.add(groupValueTranscoder.decodeStringValue(dn));
                } catch (final Exception e) {
                    LOG.debug("Caught exception resolving group from group dn {}", dn);
                }
            }
        }

        return groups;
    }

    protected final ReadableInstant getTimeValue(final LdapEntry entry, final String attribute) {
        return this.getTimeValue(entry, attribute, null);
    }

    protected final ReadableInstant getTimeValue(final LdapEntry entry, final String attribute,
                                                 final ReadableInstant defaultValue) {
        final LdapAttribute attr = entry.getAttribute(attribute);
        if (attr != null) {
            final ReadableInstant value = attr.getValue(TRANSCODER_INSTANT);
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
