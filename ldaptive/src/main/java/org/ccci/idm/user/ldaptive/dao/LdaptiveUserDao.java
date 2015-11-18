package org.ccci.idm.user.ldaptive.dao;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CRU_DESIGNATION;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_EMPLOYEE_NUMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FACEBOOKID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_FIRSTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GROUPS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_LASTNAME;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_MEMBER;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_OBJECTCLASS;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_PASSWORDCHANGEDTIME;
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
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.exception.InterruptedDaoException;
import org.ccci.idm.user.dao.ldap.AbstractLdapUserDao;
import org.ccci.idm.user.ldaptive.dao.exception.LdaptiveDaoException;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InterruptedNamingException;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class LdaptiveUserDao extends AbstractLdapUserDao {
    private static final Logger LOG = LoggerFactory.getLogger(LdaptiveUserDao.class);

    // common LDAP search filters
    private static final BaseFilter FILTER_PERSON = new EqualsFilter(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASS_PERSON);
    private static final BaseFilter FILTER_DEACTIVATED = new LikeFilter(LDAP_ATTR_CN, LDAP_DEACTIVATED_PREFIX + "*");
    private static final BaseFilter FILTER_NOT_DEACTIVATED = FILTER_DEACTIVATED.not();

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

    private int maxPageSize = 1000;

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

    public void setMaxPageSize(final int size) {
        this.maxPageSize = size;
    }

    /**
     * find {@link User} objects that match the provided filter.
     *
     * @param filter                    the LDAP search filter to use when searching
     * @param limit                     the maximum number of results to return, a limit of 0 indicates that all results
     *                                  should be returned
     * @param restrictMaxAllowedResults a flag indicating if maxSearchResults should be observed
     * @return
     * @throws ExceededMaximumAllowedResultsException exception thrown when there are more results than the maximum
     */
    @Nonnull
    private List<User> findAllByFilter(@Nullable BaseFilter filter, final boolean includeDeactivated, final int limit,
                                       final boolean restrictMaxAllowedResults)
            throws ExceededMaximumAllowedResultsException {
        final List<User> results = new ArrayList<User>();
        try {
            enqueueAllByFilter(results, filter, includeDeactivated, limit, restrictMaxAllowedResults);
            return results;
        } catch (final ExceededMaximumAllowedResultsException e) {
            // propagate ExceededMaximumAllowedResultsException exceptions
            throw e;
        } catch (final DaoException suppressed) {
            // suppress any other DaoExceptions
            return Collections.emptyList();
        }
    }

    /**
     * @param users                     collection to populate with loaded users
     * @param filter                    the LDAP search filter to use when searching
     * @param includeDeactivated        whether deactivated users should be included with the results
     * @param limit                     the maximum number of results to return, a limit of 0 indicates that all results
     *                                  should be returned
     * @param restrictMaxAllowedResults a flag indicating if maxSearchResults should be observed
     * @return number of users loaded
     * @throws DaoException
     */
    private int enqueueAllByFilter(@Nonnull final Collection<User> users, @Nullable BaseFilter filter,
                                   final boolean includeDeactivated, final int limit,
                                   final boolean restrictMaxAllowedResults) throws DaoException {
        // restrict filter as necessary
        filter = filter != null ? filter.and(FILTER_PERSON) : FILTER_PERSON;
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
            request.setReturnAttributes("*", LDAP_ATTR_PASSWORDCHANGEDTIME);

            // calculate the page size based on the provided limit & maxPageSize
            int pageSize = maxPageSize;
            if (limit != SEARCH_NO_LIMIT && pageSize > limit) {
                pageSize = limit;
            }
            if (restrictMaxAllowedResults && maxPageSize != SEARCH_NO_LIMIT && pageSize > maxPageSize + 1) {
                pageSize = maxPageSize + 1;
            }
            final PagedResultsControl prc = new PagedResultsControl(pageSize);
            request.setControls(prc);

            // retrieve results
            byte[] cookie = null;
            int processed = 0;
            do {
                // execute request
                prc.setCookie(cookie);
                cookie = null;
                final Response<SearchResult> response = search.execute(request);

                // process response
                final SearchResult result = response.getResult();
                final Iterator<LdapEntry> entries = result.getEntries().iterator();
                while ((limit == SEARCH_NO_LIMIT || processed < limit) && (!restrictMaxAllowedResults ||
                        maxSearchResults == SEARCH_NO_LIMIT || processed < maxSearchResults) && entries.hasNext()) {
                    final User user = new User();
                    userMapper.map(entries.next(), user);
                    if (users instanceof BlockingQueue) {
                        try {
                            ((BlockingQueue<User>) users).put(user);
                        } catch (final InterruptedException e) {
                            LOG.debug("Error adding user to the BlockingQueue, let's propagate the exception", e);
                            throw new InterruptedDaoException(e);
                        }
                    } else {
                        users.add(user);
                    }
                    processed++;
                }

                // have we reached our limit?
                if (limit != SEARCH_NO_LIMIT && processed >= limit) {
                    break;
                }

                // check for too many results
                if (restrictMaxAllowedResults && maxSearchResults != SEARCH_NO_LIMIT && processed >= maxSearchResults
                        && entries.hasNext()) {
                    LOG.debug("Search exceeds maxSearchResults of {}: Filter: {} Limit: {}", maxSearchResults, filter
                            .format(), limit);
                    throw new ExceededMaximumAllowedResultsException();
                }

                // process the PRC in the response
                final ResponseControl ctl = response.getControl(PagedResultsControl.OID);
                if (ctl instanceof PagedResultsControl) {
                    // get cookie for next page of results
                    cookie = ((PagedResultsControl) ctl).getCookie();
                }
            } while (cookie != null && cookie.length > 0);

            // return found users
            return processed;
        } catch (final LdapException e) {
            LOG.debug("error searching for users, wrapping & propagating exception", e);
            if (e.getCause() instanceof InterruptedNamingException) {
                throw new InterruptedDaoException(e);
            } else {
                throw new LdaptiveDaoException(e);
            }
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private User findByFilter(final BaseFilter filter, final boolean includeDeactivated) {
        try {
            final List<User> results = findAllByFilter(filter, includeDeactivated, 1, false);
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
        return findAllByFilter(new LikeFilter(LDAP_ATTR_FIRSTNAME, pattern), includeDeactivated, SEARCH_NO_LIMIT, true);
    }

    @Override
    public List<User> findAllByLastName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return findAllByFilter(new LikeFilter(LDAP_ATTR_LASTNAME, pattern), includeDeactivated, SEARCH_NO_LIMIT, true);
    }

    @Nonnull
    @Override
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return findAllByFilter(new LikeFilter(LDAP_ATTR_USERID, pattern), includeDeactivated, SEARCH_NO_LIMIT, true);

        // XXX: this is more correct, but triggers a bug in the eDirectory PagedResultsControl causing duplicate records
//        // filter = (!deactivated && cn LIKE pattern)
//        BaseFilter filter = FILTER_NOT_DEACTIVATED.and(new LikeFilter(LDAP_ATTR_CN, pattern));
//
//        // filter = (filter || (deactivated && uid LIKE pattern))
//        if (includeDeactivated) {
//            filter = filter.or(FILTER_DEACTIVATED.and(new LikeFilter(LDAP_ATTR_USERID, pattern)));
//        }
//
//        // Execute search & return results
//        return findAllByFilter(filter, includeDeactivated, SEARCH_NO_LIMIT, true);
    }

    @Nonnull
    @Override
    public List<User> findAllByGroup(@Nonnull final Group group, final boolean includeDeactivated) throws DaoException {
        // short-circuit if we can't transcode the group
        if (groupValueTranscoder == null) {
            throw new UnsupportedOperationException("Searching by group membership requires a configured Group " +
                    "ValueTranscoder");
        }

        // execute the search
        return findAllByFilter(new EqualsFilter(LDAP_ATTR_GROUPS, groupValueTranscoder.encodeStringValue(group)),
                includeDeactivated, SEARCH_NO_LIMIT, true);
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

    @Nullable
    @Override
    public User findByDesignation(@Nullable final String designation, final boolean includeDeactivated) {
        return findByFilter(new EqualsFilter(LDAP_ATTR_CRU_DESIGNATION, designation), includeDeactivated);
    }

    @Override
    public User findByEmployeeId(final String employeeId, final boolean includeDeactivated) {
        return this.findByFilter(new EqualsFilter(LDAP_ATTR_EMPLOYEE_NUMBER, employeeId), includeDeactivated);
    }

    @Override
    public int enqueueAll(@Nonnull final BlockingQueue<User> queue, final boolean includeDeactivated)
            throws DaoException {
        return enqueueAllByFilter(queue, null, includeDeactivated, SEARCH_NO_LIMIT, false);
    }

    @Override
    public void save(final User user) throws DaoException {
        assertWritable();
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
            throw convertLdapException(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void update(final User user, User.Attr... attrs) throws DaoException {
        assertWritable();
        assertValidUser(user);

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();
            this.updateInternal(conn, this.userMapper.mapDn(user), user, attrs);
        } catch (final LdapException e) {
            throw convertLdapException(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void update(final User original, final User user, final User.Attr... attrs) throws DaoException {
        assertWritable();
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
                new ModifyDnOperation(conn).execute(new ModifyDnRequest(originalDn, dn));
            }

            // update the actual user account
            this.updateInternal(conn, dn, user, attrs);
        } catch (final LdapException e) {
            throw convertLdapException(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void addToGroup(User user, Group group) throws DaoException {
        assertWritable();
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.ADD);
    }

    @Override
    public void removeFromGroup(User user, Group group) throws DaoException {
        assertWritable();
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.REMOVE);
    }

    private void modifyGroupMembership(User user, Group group, AttributeModificationType attributeModificationType)
            throws DaoException {
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
            throw convertLdapException(e);
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

    private DaoException convertLdapException(@Nonnull final LdapException e) {
        return new LdaptiveDaoException(e);
    }
}
