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
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASS_GROUP_OF_NAMES;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_OBJECTCLASS_PERSON;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.ccci.idm.user.Dn;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.SearchQuery;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.exception.InterruptedDaoException;
import org.ccci.idm.user.dao.ldap.AbstractLdapUserDao;
import org.ccci.idm.user.ldaptive.dao.exception.LdaptiveDaoException;
import org.ccci.idm.user.ldaptive.dao.filter.AndFilter;
import org.ccci.idm.user.ldaptive.dao.filter.BaseFilter;
import org.ccci.idm.user.ldaptive.dao.filter.EqualsFilter;
import org.ccci.idm.user.ldaptive.dao.filter.LikeFilter;
import org.ccci.idm.user.ldaptive.dao.filter.PresentFilter;
import org.ccci.idm.user.ldaptive.dao.util.DnUtils;
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
    private static final BaseFilter FILTER_GROUP =
            new EqualsFilter(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASS_GROUP_OF_NAMES);
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

    private String baseSearchDn = "";

    @NotNull
    private Dn baseGroupDn = Dn.ROOT;

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

    public void setBaseGroupDn(@Nullable final Dn dn) {
        baseGroupDn = dn != null ? dn : Dn.ROOT;
    }

    public void setBaseGroupDnString(@Nullable final String dn) {
        setBaseGroupDn(DnUtils.toDn(dn));
    }

    public void setMaxPageSize(final int size) {
        this.maxPageSize = size;
    }

    /**
     * find {@link User} objects that match the provided filter. This method should not be considered part of the public
     * API. This is currently exposed publicly as a quick path for advanced search in admin tools.
     *
     * @param filter                    the LDAP search filter to use when searching
     * @param limit                     the maximum number of results to return, a limit of 0 indicates that all results
     *                                  should be returned
     * @param restrictMaxAllowedResults a flag indicating if maxSearchResults should be observed
     * @return
     * @throws ExceededMaximumAllowedResultsException exception thrown when there are more results than the maximum
     */
    @Nonnull
    public List<User> findAllByFilter(@Nullable BaseFilter filter, final boolean includeDeactivated, final int limit,
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

    @Beta
    @Nonnull
    @Override
    public List<User> findAllByQuery(@Nonnull final SearchQuery query) throws DaoException {
        // build filter from search query
        final List<BaseFilter> filters = new ArrayList<BaseFilter>();
        if (!Strings.isNullOrEmpty(query.getEmail())) {
            filters.add(new LikeFilter(LDAP_ATTR_USERID, query.getEmail()));
        }
        if (!Strings.isNullOrEmpty(query.getFirstName())) {
            filters.add(new LikeFilter(LDAP_ATTR_FIRSTNAME, query.getFirstName()));
        }
        if (!Strings.isNullOrEmpty(query.getLastName())) {
            filters.add(new LikeFilter(LDAP_ATTR_LASTNAME, query.getLastName()));
        }
        if (!Strings.isNullOrEmpty(query.getEmployeeId())) {
            filters.add(new LikeFilter(LDAP_ATTR_EMPLOYEE_NUMBER, query.getEmployeeId()));
        }
        final Group group = query.getGroup();
        if (group != null) {
            // short-circuit if we can't transcode the group
            if (!group.isDescendantOfOrEqualTo(baseGroupDn)) {
                throw new UnsupportedOperationException(
                        "Cannot search by a group that is not a descendant of the baseGroupDn");
            }

            filters.add(new EqualsFilter(LDAP_ATTR_GROUPS, DnUtils.toString(group)));
        }
        final BaseFilter filter;
        if (filters.size() == 0) {
            filter = null;
        } else if (filters.size() == 1) {
            filter = filters.get(0);
        } else {
            filter = new AndFilter(filters.toArray(new BaseFilter[filters.size()]));
        }

        // execute query
        // XXX: we don't use findAllByFilter so we can properly report all DaoExceptions
        final List<User> results = new ArrayList<User>();
        enqueueAllByFilter(results, filter, query.isIncludeDeactivated(), SEARCH_NO_LIMIT, true);
        return results;
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
        if (!group.isDescendantOfOrEqualTo(baseGroupDn)) {
            throw new UnsupportedOperationException(
                    "Cannot search by a group that is not a descendant of the baseGroupDn");
        }

        // execute the search
        return findAllByFilter(new EqualsFilter(LDAP_ATTR_GROUPS, DnUtils.toString(group)), includeDeactivated,
                SEARCH_NO_LIMIT, true);
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

            final String originalDn = this.userMapper.mapDn(original);
            final String dn;
            if (FluentIterable.of(attrs).contains(User.Attr.EMAIL)) {
                // modify the DN if we are updating the user's email and it changed
                dn = this.userMapper.mapDn(user);
                if (!Objects.equal(originalDn, dn)) {
                    new ModifyDnOperation(conn).execute(new ModifyDnRequest(originalDn, dn));
                }
            } else {
                dn = originalDn;
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
    public void addToGroup(@Nonnull final User user, @Nonnull final Group group) throws DaoException {
        assertWritable();
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.ADD);
    }

    @Override
    public void removeFromGroup(@Nonnull final User user, @Nonnull final Group group) throws DaoException {
        assertWritable();
        assertValidUser(user);

        modifyGroupMembership(user, group, AttributeModificationType.REMOVE);
    }

    /**
     * Returns all available groups
     *
     * Note that this method is not particular to a user, but is temporarily made available here until a
     * more suitable framework becomes available for providing group dao.
     *
     * @param baseSearchDn
     *  null value indicates to return all groups
     *
     * @return list of all available groups under base search dn
     */
    @Nonnull
    @Override
    public List<Group> getAllGroups(@Nullable final Dn baseSearchDn) throws DaoException {
        final List<Group> groups = Lists.newArrayList();
        enqueueGroupsByFilter(groups, null, baseSearchDn, SEARCH_NO_LIMIT, false);
        return groups;
    }

    /**
     * @param groups                    collection to populate with loaded groups
     * @param filter                    the LDAP search filter to use when searching
     * @param baseSearchDn        groups base search dn, null defaults to all groups (this.baseGroupDn)
     * @param limit                     the maximum number of results to return, a limit of 0 indicates that all results
     *                                  should be returned
     * @param restrictMaxAllowedResults a flag indicating if maxSearchResults should be observed
     * @return number of users loaded
     * @throws DaoException
     */
    private int enqueueGroupsByFilter(@Nonnull final Collection<Group> groups, @Nullable BaseFilter filter,
                                      @Nullable final Dn baseSearchDn, final int limit,
                                      final boolean restrictMaxAllowedResults) throws DaoException {
        // require provided base dn be descendant of (or identical to) groups base dn, under threat of exception
        final Dn searchDn = MoreObjects.firstNonNull(baseSearchDn, baseGroupDn);
        if (!searchDn.isDescendantOfOrEqualTo(baseGroupDn)) {
            throw new IllegalArgumentException(baseSearchDn + " must be descendant of (or identical to) " +
                    baseGroupDn);
        }

        filter = filter != null ? filter.and(FILTER_GROUP) : FILTER_GROUP;

        // perform search
        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();
            SearchOperation search = new SearchOperation(conn);
            final SearchRequest request = new SearchRequest(DnUtils.toString(searchDn), filter);

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
                    final Dn dn = DnUtils.toDnSafe(entries.next().getDn());
                    if (dn != null && dn.isDescendantOfOrEqualTo(baseGroupDn) && dn.getComponents().size() > 0) {
                        if (groups instanceof BlockingQueue) {
                            try {
                                ((BlockingQueue<Group>) groups).put(dn.asGroup());
                            } catch (final InterruptedException e) {
                                LOG.debug("Error adding group to the BlockingQueue, let's propagate the exception", e);
                                throw new InterruptedDaoException(e);
                            }
                        } else {
                            groups.add(dn.asGroup());
                        }
                    }
                    processed++;
                }

                // process the PRC in the response
                final ResponseControl ctl = response.getControl(PagedResultsControl.OID);
                if (ctl instanceof PagedResultsControl) {
                    // get cookie for next page of results
                    cookie = ((PagedResultsControl) ctl).getCookie();
                }
            } while (cookie != null && cookie.length > 0);

            // return found groups
            return processed;
        } catch (final LdapException e) {
            LOG.debug("error searching for groups, wrapping & propagating exception", e);
            if (e.getCause() instanceof InterruptedNamingException) {
                throw new InterruptedDaoException(e);
            } else {
                throw new LdaptiveDaoException(e);
            }
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private void modifyGroupMembership(User user, Group group, AttributeModificationType attributeModificationType)
            throws DaoException {
        if (!group.isDescendantOfOrEqualTo(baseGroupDn)) {
            throw new UnsupportedOperationException(
                    "Cannot modify group membership for a group that is not a descendant of the baseGroupDn");
        }

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            String userDn = this.userMapper.mapDn(user);
            final String groupDn = DnUtils.toString(group);

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
