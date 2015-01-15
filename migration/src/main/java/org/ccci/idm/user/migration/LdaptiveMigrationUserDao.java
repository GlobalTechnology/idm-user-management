package org.ccci.idm.user.migration;

import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_CN;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_THEKEY_GUID;
import static org.ccci.idm.user.dao.ldap.Constants.LDAP_ATTR_USERID;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.ldaptive.dao.LdaptiveUserDao;
import org.ccci.idm.user.ldaptive.dao.filter.BaseFilter;
import org.ccci.idm.user.ldaptive.dao.filter.EqualsFilter;
import org.ccci.idm.user.ldaptive.dao.filter.PresentFilter;
import org.ccci.idm.user.ldaptive.dao.util.LdapUtils;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModificationType;
import org.ldaptive.Connection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LdaptiveMigrationUserDao extends LdaptiveUserDao implements MigrationUserDao {
    private static final Logger LOG = LoggerFactory.getLogger(LdaptiveMigrationUserDao.class);

    @NotNull
    protected LdapEntryMapper<User> legacyKeyUserMapper;

    private String legacyKeyBaseSearchDn = "";

    public void setLegacyKeyUserMapper(LdapEntryMapper<User> legacyKeyUserMapper) {
        this.legacyKeyUserMapper = legacyKeyUserMapper;
    }

    public void setLegacyKeyBaseSearchDn(final String baseDn) {
        this.legacyKeyBaseSearchDn = baseDn;
    }

    @Override
    public synchronized void moveLegacyKeyUser(final User user) {
        this.moveLegacyKeyUser(user, user.getEmail());
    }

    @Override
    public synchronized void moveLegacyKeyUser(final User user, final String newEmail) {
        final boolean changingEmail = !newEmail.equals(user.getEmail());

        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            // modify the DN if it changed
            final String legacyDn = this.legacyKeyUserMapper.mapDn(user);
            if(changingEmail) {
                user.setEmail(newEmail, newEmail.equalsIgnoreCase(user.getEmail()) && user.isEmailVerified());
            }
            final String dn = this.userMapper.mapDn(user);
            if (!Objects.equal(legacyDn, dn)) {
                new ModifyDnOperation(conn).execute(new ModifyDnRequest(legacyDn, dn));
            }

            // update email if changing email
            if(changingEmail) {
                this.updateInternal(conn, dn, user, User.Attr.EMAIL);
            }
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    @Override
    public void updateGuid(final User user) {
        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            // generate the LdapEntry for this user
            final LdapEntry entry = new LdapEntry();
            this.userMapper.map(user, entry);

            // execute the ModifyOperation
            final String dn = this.userMapper.mapDn(user);
            new ModifyOperation(conn).execute(new ModifyRequest(dn, new AttributeModification
                    (AttributeModificationType.REPLACE, entry.getAttribute(LDAP_ATTR_GUID))));
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private List<User> findAllLegacyKeyByFilter(BaseFilter filter, final boolean includeDeactivated) {
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

            // build & execute search
            final SearchOperation search = new SearchOperation(conn);
            final Response<SearchResult> response = search.execute(new SearchRequest(this.legacyKeyBaseSearchDn,
                    filter));
            final SearchResult result = response.getResult();

            // process response
            final List<User> users = new ArrayList<User>();
            for (final LdapEntry entry : result.getEntries()) {
                final User user = new User();
                this.legacyKeyUserMapper.map(entry, user);
                users.add(user);
            }

            // return found users
            return users;
        } catch (final LdapException e) {
            LOG.debug("error searching for users, returning an empty list", e);
            return Collections.emptyList();
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    private User findLegacyKeyByFilter(final BaseFilter filter, final boolean includeDeactivated) {
        final List<User> results = this.findAllLegacyKeyByFilter(filter, includeDeactivated);
        return results.size() > 0 ? results.get(0) : null;
    }

    @Override
    public User findLegacyKeyByTheKeyGuid(final String guid, final boolean includeDeactivated) {
        // theKeyGuid == {guid} || (guid == {guid} && theKeyGuid == null)
        return this.findLegacyKeyByFilter(new EqualsFilter(LDAP_ATTR_THEKEY_GUID, guid).or(new EqualsFilter
                (LDAP_ATTR_GUID, guid).and(new PresentFilter(LDAP_ATTR_THEKEY_GUID).not())), includeDeactivated);
    }

    @Override
    public User findLegacyKeyByEmail(final String email, final boolean includeDeactivated) {
        // filter = (!deactivated && cn = email)
        BaseFilter filter = FILTER_NOT_DEACTIVATED.and(new EqualsFilter(LDAP_ATTR_CN, email));

        // filter = (filter || (deactivated && uid = email))
        if (includeDeactivated) {
            filter = filter.or(FILTER_DEACTIVATED.and(new EqualsFilter(LDAP_ATTR_USERID, email)));
        }

        // Execute search & return results
        return this.findLegacyKeyByFilter(filter, includeDeactivated);
    }

    @Override
    public List<User> findAll(final boolean includeDeactivated) throws ExceededMaximumAllowedResultsException {
        return this.findAllByFilter(FILTER_PERSON, includeDeactivated, 0, false);
    }

    @Override
    public List<User> findAllLegacyKeyUsers(final boolean includeDeactivated) {
        return this.findAllLegacyKeyByFilter(FILTER_PERSON, includeDeactivated);
    }
}
