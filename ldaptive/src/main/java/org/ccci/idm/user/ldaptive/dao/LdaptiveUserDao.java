package org.ccci.idm.user.ldaptive.dao;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMPLOYEE_NUMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GROUPS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_MEMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RELAY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_DEACTIVATED_PREFIX;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASS_PERSON;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.ldap.AbstractLdapUserDao;
import org.ccci.idm.user.ldaptive.dao.filter.BaseFilter;
import org.ccci.idm.user.ldaptive.dao.filter.EqualsFilter;
import org.ccci.idm.user.ldaptive.dao.filter.LikeFilter;
import org.ccci.idm.user.ldaptive.dao.filter.PresentFilter;
import org.ccci.idm.user.ldaptive.dao.util.LdapUtils;
import org.ldaptive.AddOperation;
import org.ldaptive.AddRequest;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModificationType;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ModifyDnOperation;
import org.ldaptive.ModifyDnRequest;
import org.ldaptive.ModifyOperation;
import org.ldaptive.ModifyRequest;
import org.ldaptive.Response;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.beans.LdapEntryMapper;
import org.ldaptive.control.PagedResultsControl;
import org.ldaptive.control.ResponseControl;
import org.ldaptive.io.ValueTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LdaptiveUserDao extends AbstractLdapUserDao {
    private static final Logger LOG = LoggerFactory.getLogger(LdaptiveUserDao.class);

    // common LDAP search filters
    protected static final BaseFilter FILTER_PERSON = new EqualsFilter(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASS_PERSON);
    protected static final BaseFilter FILTER_DEACTIVATED = new LikeFilter(LDAP_ATTR_CN, LDAP_DEACTIVATED_PREFIX + "*");
    protected static final BaseFilter FILTER_NOT_DEACTIVATED = FILTER_DEACTIVATED.not();

    // Predicates used for filtering objects
    private static final Predicate<LdapAttribute> PREDICATE_EMPTY_ATTRIBUTE = new Predicate<LdapAttribute>() {
        @Override
        public boolean apply(final LdapAttribute input) {
            return input.size() == 0;
        }
    };

    @NotNull
    protected ConnectionFactory connectionFactory;

    @NotNull
    protected LdapEntryMapper<User> userMapper;

    @Nullable
    protected ValueTranscoder<Group> groupValueTranscoder;

    private String baseSearchDn = "";

    public void setConnectionFactory(final ConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public void setUserMapper(final LdapEntryMapper<User> mapper) {
        this.userMapper = mapper;
    }

    public void setBaseSearchDn(final String dn) {
        this.baseSearchDn = dn;
    }

    public void setGroupValueTranscoder(@Nullable ValueTranscoder<Group> transcoder) {
        this.groupValueTranscoder = transcoder;
    }

    /**
     * find {@link User} objects that match the provided filter.
     *
     * @param filter             the LDAP search filter to use when searching
     * @param includeDeactivated Whether deactivated accounts should be included in the search
     * @param limit              the maximum number of results to return
     * @return
     */
    protected List<User> findAllByFilter(BaseFilter filter, final boolean includeDeactivated,
                                       final int limit) throws ExceededMaximumAllowedResultsException {
        // restrict filter as necessary
        filter = filter.and(FILTER_PERSON);
        if (!includeDeactivated) {
            filter = filter.and(FILTER_NOT_DEACTIVATED);
        }

        // perform search
        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();
            SearchOperation search = new SearchOperation(conn);
            final SearchRequest request = new SearchRequest(this.baseSearchDn, filter);

            // calculate the actual limit based on the provided limit & maxSearchResults
            final int actualLimit = limit == SEARCH_NO_LIMIT ? maxSearchResults : maxSearchResults == SEARCH_NO_LIMIT
                    ? limit : limit > maxSearchResults ? maxSearchResults : limit;
            if (actualLimit != SEARCH_NO_LIMIT) {
                request.setControls(new PagedResultsControl(actualLimit));
            }

            // execute search
            final Response<SearchResult> response = search.execute(request);
            final SearchResult result = response.getResult();

            // check for too many results when we are limiting results
            if (maxSearchResults != SEARCH_NO_LIMIT && actualLimit != SEARCH_NO_LIMIT) {
                final ResponseControl ctl = response.getControl(PagedResultsControl.OID);
                if (ctl instanceof PagedResultsControl) {
                    final PagedResultsControl prc = (PagedResultsControl) ctl;
                    if ((limit == SEARCH_NO_LIMIT || limit > maxSearchResults) && prc.getSize() > maxSearchResults) {
                        LOG.error("Search exceeds maxSearchResults of {}: Filter: {} Limit: {} Found Results: " +
                                "{}", maxSearchResults, filter.format(), limit, prc.getSize());
                        throw new ExceededMaximumAllowedResultsException();
                    }
                }
            }

            // process response
            int count = 0;
            final List<User> users = new ArrayList<User>();
            for (final LdapEntry entry : result.getEntries()) {
                final User user = new User();
                userMapper.map(entry, user);
                users.add(user);

                count++;
                if (count % 2000 == 0) {
                    LOG.info("loaded {} users", count);
                }
            }

            // return found users
            return users;
        } catch (final LdapException e) {
            LOG.info("error searching for users, returning an empty list", e);
            return Collections.emptyList();
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private User findByFilter(final BaseFilter filter, final boolean includeDeactivated) {
        try {
            final List<User> results = this.findAllByFilter(filter, includeDeactivated, 1);
            return results.size() > 0 ? results.get(0) : null;
        } catch (final ExceededMaximumAllowedResultsException e) {
            // this should be unreachable, but if we do reach it, log the exception and propagate the exception
            LOG.error("ExceededMaximumAllowedResults thrown for findByFilter, this should be impossible!!!!", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<User> findAllByFirstName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new LikeFilter(LDAP_ATTR_FIRSTNAME, pattern), includeDeactivated, SEARCH_NO_LIMIT);
    }

    @Override
    public List<User> findAllByLastName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new LikeFilter(LDAP_ATTR_LASTNAME, pattern), includeDeactivated, SEARCH_NO_LIMIT);
    }

    @Override
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        // filter = (!deactivated && cn LIKE pattern)
        BaseFilter filter = FILTER_NOT_DEACTIVATED.and(new LikeFilter(LDAP_ATTR_CN, pattern));

        // filter = (filter || (deactivated && uid LIKE pattern))
        if (includeDeactivated) {
            filter = filter.or(FILTER_DEACTIVATED.and(new LikeFilter(LDAP_ATTR_USERID, pattern)));
        }

        // Execute search & return results
        return this.findAllByFilter(filter, includeDeactivated, SEARCH_NO_LIMIT);
    }

    @Override
    public User findByGuid(final String guid, final boolean includeDeactivated) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_GUID, guid), includeDeactivated);
    }

    @Override
    public User findByRelayGuid(final String guid, final boolean includeDeactivated) {
        // relayGuid == {guid} || (guid == {guid} && relayGuid == null)
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_RELAY_GUID, guid).or(new EqualsFilter(LDAP_ATTR_GUID,
                guid).and(new PresentFilter(LDAP_ATTR_RELAY_GUID).not())), includeDeactivated);
    }

    @Override
    public User findByTheKeyGuid(final String guid, final boolean includeDeactivated) {
        // theKeyGuid == {guid} || (guid == {guid} && theKeyGuid == null)
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_THEKEY_GUID, guid).or(new EqualsFilter(LDAP_ATTR_GUID,
                guid).and(new PresentFilter(LDAP_ATTR_THEKEY_GUID).not())), includeDeactivated);
    }

    @Override
    public User findByFacebookId(final String id, final boolean includeDeactivated) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_FACEBOOKID, id), includeDeactivated);
    }

    @Override
    public User findByEmail(final String email, final boolean includeDeactivated) {
        // filter = (!deactivated && cn = email)
        BaseFilter filter = FILTER_NOT_DEACTIVATED.and(new EqualsFilter(LDAP_ATTR_CN, email));

        // filter = (filter || (deactivated && uid = email))
        if (includeDeactivated) {
            filter = filter.or(FILTER_DEACTIVATED.and(new EqualsFilter(LDAP_ATTR_USERID, email)));
        }

        // Execute search & return results
        return this.findByFilter(filter, includeDeactivated);
    }

    @Override
    public User findByEmployeeId(final String employeeId, final boolean includeDeactivated) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_EMPLOYEE_NUMBER, employeeId), includeDeactivated);
    }

    @Override
    public void save(final User user) {
        assertValidUser(user);

        // attempt saving the user
        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();
            final AddOperation add = new AddOperation(conn);
            final LdapEntry entry = new LdapEntry();
            this.userMapper.map(user, entry);
            add.execute(new AddRequest(this.userMapper.mapDn(user), Collections2.filter(entry.getAttributes(),
                    Predicates.not(PREDICATE_EMPTY_ATTRIBUTE))));
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void update(final User user, User.Attr... attrs) {
        assertValidUser(user);

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();
            this.updateInternal(conn, this.userMapper.mapDn(user), user, attrs);
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void update(final User original, final User user, final User.Attr... attrs) {
        assertValidUser(original);
        assertValidUser(user);

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            // modify the DN if it changed
            final String dn = this.userMapper.mapDn(user);
            final String originalDn = this.userMapper.mapDn(original);
            if (!Objects.equal(originalDn, dn)) {
                synchronized (this) {
                    new ModifyDnOperation(conn).execute(new ModifyDnRequest(originalDn, dn));
                }
            }

            // update the actual user account
            this.updateInternal(conn, dn, user, attrs);
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void addToGroup(User user, Group group) {
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.ADD);
    }

    @Override
    public void removeFromGroup(User user, Group group) {
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.REMOVE);
    }

    private void modifyGroupMembership(User user, Group group, AttributeModificationType attributeModificationType) {
        if (this.groupValueTranscoder == null) {
            throw new UnsupportedOperationException("Modifying group membership requires a configured Group ValueTranscoder");
        }

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            String userDn = this.userMapper.mapDn(user);
            String groupDn = this.groupValueTranscoder.encodeStringValue(group);

            // modify user entry
            modifyEntry(conn, userDn, attributeModificationType, groupDn, LDAP_ATTR_GROUPS);

            // modify group entry
            modifyEntry(conn, groupDn, attributeModificationType, userDn, LDAP_ATTR_MEMBER);

        } catch (final LdapException e) {
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    protected void updateInternal(final Connection conn, final String dn, final User user,
                                User.Attr... attrs) throws LdapException {
        // generate the list of modifications to make for this account
        final ArrayList<AttributeModification> modifications = new ArrayList<AttributeModification>();

        // update attributes set on the user model
        final LdapEntry entry = new LdapEntry();
        this.userMapper.map(user, entry);
        final Set<String> mask = this.getAttributeMask(attrs);

        for (final LdapAttribute attribute : entry.getAttributes()) {
            if (mask.contains(attribute.getName())) {
                modifications.add(new AttributeModification(AttributeModificationType.REPLACE, attribute));
            }
        }

        // execute the ModifyOperation
        new ModifyOperation(conn).execute(new ModifyRequest(dn, modifications.toArray(new
                AttributeModification[modifications.size()])));
    }

    private void modifyEntry(final Connection conn, final String dn,
                             AttributeModificationType attributeModificationType,
                             String attributeValue,
                             String... attributeNames) throws LdapException {

        final ArrayList<AttributeModification> modifications = new ArrayList<AttributeModification>();

        for(String attributeName : attributeNames) {
            LdapAttribute ldapAttribute = new LdapAttribute(attributeName, attributeValue);
            modifications.add(new AttributeModification(attributeModificationType, ldapAttribute));
        }

        new ModifyOperation(conn).execute(new ModifyRequest(dn,
                modifications.toArray(new AttributeModification[modifications.size()])));
    }
}
