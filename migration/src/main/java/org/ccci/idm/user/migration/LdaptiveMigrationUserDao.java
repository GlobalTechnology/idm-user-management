package org.ccci.idm.user.migration;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import org.ccci.idm.user.User;
import org.ccci.idm.user.ldaptive.dao.LdaptiveUserDao;
import org.ccci.idm.user.ldaptive.dao.util.LdapUtils;
import org.ldaptive.Connection;
import org.ldaptive.LdapException;
import org.ldaptive.ModifyDnOperation;
import org.ldaptive.ModifyDnRequest;
import org.ldaptive.beans.LdapEntryMapper;

import javax.validation.constraints.NotNull;

public class LdaptiveMigrationUserDao extends LdaptiveUserDao implements MigrationUserDao {
    @NotNull
    protected LdapEntryMapper<User> legacyKeyUserMapper;

    @Override
    public void moveLegacyKeyUser(final User user) {
        Connection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.open();

            // modify the DN if it changed
            final String legacyDn = this.legacyKeyUserMapper.mapDn(user);
            final String dn = this.userMapper.mapDn(user);
            if (!Objects.equal(legacyDn, dn)) {
                new ModifyDnOperation(conn).execute(new ModifyDnRequest(legacyDn, dn));
            }
        } catch (final LdapException e) {
            // XXX: for now just propagate any exceptions as RuntimeExceptions
            throw Throwables.propagate(e);
        } finally {
            LdapUtils.closeConnection(conn);
        }
    }

    public void setLegacyKeyUserMapper(LdapEntryMapper<User> legacyKeyUserMapper)
    {
        this.legacyKeyUserMapper = legacyKeyUserMapper;
    }
}
