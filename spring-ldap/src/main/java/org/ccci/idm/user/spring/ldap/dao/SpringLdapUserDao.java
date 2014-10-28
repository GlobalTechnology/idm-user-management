package org.ccci.idm.user.spring.ldap.dao;

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

import com.google.common.base.Throwables;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.ldap.AbstractLdapUserDao;
import org.ccci.idm.user.spring.ldap.dao.mapper.UserAttributesMapper;
import org.ccci.idm.user.spring.ldap.dao.util.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AggregateDirContextProcessor;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.NotFilter;
import org.springframework.ldap.filter.NotPresentFilter;
import org.springframework.ldap.filter.OrFilter;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpringLdapUserDao extends AbstractLdapUserDao {
    private static final Logger LOG = LoggerFactory.getLogger(SpringLdapUserDao.class);

    // common LDAP search filters
    private static final Filter FILTER_PERSON = new EqualsFilter(LDAP_ATTR_OBJECTCLASS, LDAP_OBJECTCLASS_PERSON);
    private static final Filter FILTER_DEACTIVATED = new LikeFilter(LDAP_ATTR_CN, LDAP_DEACTIVATED_PREFIX + "*");

    private static final UserAttributesMapper MAPPER = new UserAttributesMapper();

    @NotNull
    private LdapTemplate ldapTemplate;

    @NotNull
    private String modelDn;

    public void setLdapTemplate(final LdapTemplate template) {
        this.ldapTemplate = template;
    }

    public void setModelDn(final String dn) {
        this.modelDn = dn;
    }

    /**
     * Find all users matching the pattern specified in the filter.
     *
     * @param filter Filter for the LDAP search.
     * @param limit  limit the number of returned results to this amount
     * @return {@link java.util.List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    private List<User> findAllByFilter(final Filter filter, final int limit) throws
            ExceededMaximumAllowedResultsException {
        final String encodedFilter = filter.encode();

        // set the actual limit based on the maxLimit
        final int actualLimit = limit == SEARCH_NO_LIMIT ? maxSearchResults : maxSearchResults == SEARCH_NO_LIMIT ?
                limit : limit > maxSearchResults ? maxSearchResults : limit;
        LOG.debug("Find: Limit: {} Actual Limit: {} Filter: {}", limit, actualLimit, encodedFilter);

        // Initialize various search filters
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final AggregateDirContextProcessor processor = new AggregateDirContextProcessor();

        // Limit number of returned results when necessary
        PagedResultsDirContextProcessor pager = null;
        if (actualLimit != SEARCH_NO_LIMIT) {
            pager = new PagedResultsDirContextProcessor(actualLimit);
            processor.addDirContextProcessor(pager);
        }

        // Execute LDAP query
        final List<?> rawResults = ldapTemplate.search("", encodedFilter, controls, MAPPER, processor);
        if (LOG.isDebugEnabled() && pager != null) {
            LOG.debug("Found Results: {}", pager.getResultSize());
        }

        // check for too many results when we are limiting results
        if (maxSearchResults != SEARCH_NO_LIMIT && actualLimit != SEARCH_NO_LIMIT) {
            if ((limit == SEARCH_NO_LIMIT || limit > maxSearchResults) && pager.getResultSize() > maxSearchResults) {
                LOG.error("Search exceeds maxSearchResults of {}: Filter: {} Limit: {} Found Results: {}",
                        maxSearchResults, encodedFilter, limit, pager.getResultSize());
                throw new ExceededMaximumAllowedResultsException();
            }
        }

        // Filter results to make sure only GcxUser objects are returned
        final ArrayList<User> results = new ArrayList<User>();
        for (Object user : rawResults) {
            if (user instanceof User) {
                results.add((User) user);
            }
        }

        // return filtered users
        return results;
    }

    /**
     * searches for the first User that matches the specified filter
     *
     * @param filter
     * @return
     */
    private User findByFilter(final Filter filter) {
        try {
            final List<User> results = this.findAllByFilter(filter, 1);
            return results.size() > 0 ? results.get(0) : null;
        } catch (final ExceededMaximumAllowedResultsException e) {
            // this should be unreachable, but if we do reach it, log the exception and propagate it
            LOG.error("ExceededMaximumAllowedResults thrown for findByFilter, this should be impossible!!!!", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public User findByGuid(final String guid) {
        return this.findByFilter(new AndFilter().and(new EqualsFilter(LDAP_ATTR_GUID, guid)).and(FILTER_PERSON));
    }

    @Override
    public User findByRelayGuid(final String guid) {
        // relayGuid == {guid} || (guid == {guid} && relayGuid == null)
        return this.findByFilter(new AndFilter().and(new OrFilter().or(new EqualsFilter(LDAP_ATTR_RELAY_GUID,
                guid)).or(new AndFilter().and(new EqualsFilter(LDAP_ATTR_GUID,
                guid)).and(new NotPresentFilter(LDAP_ATTR_RELAY_GUID)))).and(FILTER_PERSON));
    }

    @Override
    public User findByTheKeyGuid(final String guid) {
        // theKeyGuid == {guid} || (guid == {guid} && theKeyGuid == null)
        return this.findByFilter(new AndFilter().and(new OrFilter().or(new EqualsFilter(LDAP_ATTR_THEKEY_GUID,
                guid)).or(new AndFilter().and(new EqualsFilter(LDAP_ATTR_GUID,
                guid)).and(new NotPresentFilter(LDAP_ATTR_THEKEY_GUID)))).and(FILTER_PERSON));
    }

    @Override
    public User findByEmail(final String email) {
        return this.findByFilter(new AndFilter().and(new EqualsFilter(LDAP_ATTR_EMAIL,
                email)).and(new NotFilter(FILTER_DEACTIVATED)).and(FILTER_PERSON));
    }

    @Override
    public User findByFacebookId(final String id) {
        return this.findByFilter(new AndFilter().and(new EqualsFilter(LDAP_ATTR_FACEBOOKID, id)).and(FILTER_PERSON));
    }

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern Pattern used for matching first name.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    @Override
    public List<User> findAllByFirstName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new AndFilter().and(new LikeFilter(LDAP_ATTR_FIRSTNAME,
                pattern)).and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern Pattern used for matching last name.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    @Override
    public List<User> findAllByLastName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(new AndFilter().and(new LikeFilter(LDAP_ATTR_LASTNAME,
                pattern)).and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    /**
     * Find all users matching the userid pattern.
     *
     * @param pattern            Pattern used for matching userids.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    @Override
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        // filter = (!deactivated && cn LIKE pattern)
        Filter filter = new AndFilter().and(new NotFilter(FILTER_DEACTIVATED)).and(new LikeFilter(LDAP_ATTR_CN,
                pattern));

        // filter = (filter || (deactivated && uid LIKE pattern))
        if (includeDeactivated) {
            filter = new OrFilter().or(filter).or(new AndFilter().and(FILTER_DEACTIVATED).and(new LikeFilter
                    (LDAP_ATTR_USERID, pattern)));
        }

        // Execute search & return results
        return this.findAllByFilter(new AndFilter().and(filter).and(FILTER_PERSON), SEARCH_NO_LIMIT);
    }

    @Override
    public void save(final User user) {
        this.assertValidUser(user);

        try {
            final Name dn = this.generateModelDn(user);
            final Attributes attrs = MAPPER.mapToAttributes(user);

            this.ldapTemplate.bind(dn, null, attrs);
        } catch (final InvalidNameException e) {
            //XXX: for now propagate exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void update(final User user, final User.Attr... attrs) {
        this.assertValidUser(user);
        try {
            this.updateInternal(this.generateModelDn(user), user, attrs);
        } catch (final NamingException e) {
            //XXX: for now propagate exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void update(final User original, final User user, final User.Attr... attrs) {
        this.assertValidUser(original);
        this.assertValidUser(user);

        try {
            // rename the dn has changed
            final Name dn = this.generateModelDn(user);
            final Name originalDn = this.generateModelDn(original);
            if (!dn.equals(originalDn)) {
                ldapTemplate.rename(originalDn, dn);
            }

            // update the user in LDAP
            this.updateInternal(dn, user, attrs);
        } catch (final NamingException e) {
            //XXX: for now propagate exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        }
    }

    private void updateInternal(final Name dn, final User user, final User.Attr... attrs) throws NamingException {
        final Set<String> mask = this.getAttributeMask(attrs);

        // map attributes onto directory context
        final DirContextOperations ctx = ldapTemplate.lookupContext(dn);
        for (final Attribute attr : Collections.list(MAPPER.mapToAttributes(user).getAll())) {
            if (mask.contains(attr.getID())) {
                ctx.setAttributeValues(attr.getID(), Collections.list(attr.getAll()).toArray(new Object[attr.size()]));
            }
        }

        // execute update
        ldapTemplate.modifyAttributes(ctx);
    }

    private Name generateModelDn(final User user) throws InvalidNameException {
        final String uid;
        if (!user.isDeactivated()) {
            uid = user.getEmail();
        } else if (user.getDeactivatedUid() != null) {
            uid = user.getDeactivatedUid();
        } else {
            uid = LDAP_DEACTIVATED_PREFIX + user.getGuid();
            user.setDeactivatedUid(uid);
        }

        return new LdapName(LdapUtils.getFilterWithValues(this.modelDn, uid));
    }
}
