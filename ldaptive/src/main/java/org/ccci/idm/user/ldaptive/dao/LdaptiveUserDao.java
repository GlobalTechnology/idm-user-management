package org.ccci.idm.user.ldaptive.dao;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMAIL;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_RELAY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_DEACTIVATED_PREFIX;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASS_PERSON;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
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
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.beans.LdapEntryMapper;
import org.ldaptive.control.PagedResultsControl;
import org.ldaptive.control.ResponseControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LdaptiveUserDao extends AbstractLdapUserDao {
    private static final Logger LOG = LoggerFactory.getLogger(LdaptiveUserDao.class);

    // common LDAP search filters
    private static final EqualsFilter FILTER_PERSON = new EqualsFilter(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASS_PERSON);
    private static final LikeFilter FILTER_DEACTIVATED = new LikeFilter(LDAP_ATTR_CN, LDAP_DEACTIVATED_PREFIX + "*");

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

    /**
     * find {@link User} objects that match the provided filter.
     *
     * @param filter the LDAP search filter to use when searching
     * @param limit  the maximum number of results to return
     * @return
     */
    private List<User> findAllByFilter(final SearchFilter filter,
                                       final int limit) throws ExceededMaximumAllowedResultsException {
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
            final List<User> users = new ArrayList<User>();
            for (final LdapEntry entry : result.getEntries()) {
                final User user = new User();
                userMapper.map(entry, user);
                users.add(user);
            }

            // return found users
            return users;
        } catch (final LdapException e) {
            return Collections.emptyList();
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private User findByFilter(final SearchFilter filter) {
        try {
            final List<User> results = this.findAllByFilter(filter, 1);
            return results.size() > 0 ? results.get(0) : null;
        } catch (final ExceededMaximumAllowedResultsException e) {
            // this should be unreachable, but if we do reach it, log the exception and propagate the exception
            LOG.error("ExceededMaximumAllowedResults thrown for findByFilter, this should be impossible!!!!", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<User> findAllByFirstName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new LikeFilter(LDAP_ATTR_FIRSTNAME, pattern).and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    @Override
    public List<User> findAllByLastName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new LikeFilter(LDAP_ATTR_LASTNAME, pattern).and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    @Override
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        // filter = (!deactivated && cn LIKE pattern)
        BaseFilter filter = FILTER_DEACTIVATED.not().and(new LikeFilter(LDAP_ATTR_CN, pattern));

        // filter = (filter || (deactivated && uid LIKE pattern))
        if (includeDeactivated) {
            filter = filter.or(FILTER_DEACTIVATED.and(new LikeFilter(LDAP_ATTR_USERID, pattern)));
        }

        // Execute search & return results
        return this.findAllByFilter(filter.and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    @Override
    public User findByGuid(final String guid) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_GUID, guid).and(FILTER_PERSON));
    }

    @Override
    public User findByRelayGuid(final String guid) {
        // relayGuid == {guid} || (guid == {guid} && relayGuid == null)
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_RELAY_GUID, guid).or(new EqualsFilter(LDAP_ATTR_GUID,
                guid).and(new PresentFilter(LDAP_ATTR_RELAY_GUID).not())).and(FILTER_PERSON));
    }

    @Override
    public User findByTheKeyGuid(final String guid) {
        // theKeyGuid == {guid} || (guid == {guid} && theKeyGuid == null)
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_THEKEY_GUID, guid).or(new EqualsFilter(LDAP_ATTR_GUID,
                guid).and(new PresentFilter(LDAP_ATTR_THEKEY_GUID).not())).and(FILTER_PERSON));
    }

    @Override
    public User findByFacebookId(final String id) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_FACEBOOKID, id).and(FILTER_PERSON));
    }

    @Override
    public User findByEmail(final String email) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_EMAIL, email).and(FILTER_DEACTIVATED.not()).and
                (FILTER_PERSON));
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
            this.updateInternal(conn, this.userMapper.mapDn(user), user);
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
            if (!Objects.equals(originalDn, dn)) {
                new ModifyDnOperation(conn).execute(new ModifyDnRequest(originalDn, dn));
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

    private void updateInternal(final Connection conn, final String dn, final User user,
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
}
