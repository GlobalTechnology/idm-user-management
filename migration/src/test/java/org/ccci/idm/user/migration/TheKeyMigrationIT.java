package org.ccci.idm.user.migration;

import static org.junit.Assume.assumeNotNull;

import org.ccci.idm.user.User;
import org.ccci.idm.user.exception.UserException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/org/ccci/idm/user/ldaptive/dao/ldap.xml",
        "/org/ccci/idm/user/ldaptive/dao/config.xml", "dao-migration.xml", "usermanager.xml"})
public class TheKeyMigrationIT {
    private static final Logger LOG = LoggerFactory.getLogger(TheKeyMigrationIT.class);

    @Inject
    MigrationUserManager userManager;

    @Inject
    private MigrationUserDao dao;

    // required config values for tests to pass successfully
    @Value("${ldap.url:#{null}}")
    private String url = null;
    @Value("${ldap.base:#{null}}")
    private String base = null;
    @Value("${ldap.userdn:#{null}}")
    private String username = null;
    @Value("${ldap.password:#{null}}")
    private String password = null;
    @Value("${ldap.dn.user:#{null}}")
    private String dn = null;

    private void assumeConfigured() throws Exception {
        assumeNotNull(url, base, username, password, dn);
        assumeNotNull(dao, userManager);
    }

    @Test
    @Ignore
    public void migrateTheKeyUsers() throws Exception {
        assumeConfigured();

        // fetch all remaining un-migrated Key users
        final List<User> users = userManager.getAllLegacyUsers(true);

        // migrate users via thread pool
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        for (final User user : users) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // clear any conflicting guids if possible
                    if (!clearConflictingGuids(user)) {
                        return;
                    }

                    // deactivate user if there is a conflicting email address
                    if(!user.isDeactivated() && userManager.doesEmailExist(user.getEmail())) {
                        // TODO: deactivate this user before moving
                        return;
                    }

                    // move user
                    userManager.moveLegacyKeyUser(user);

                    // populate ccciGuid
                    populateGuid(user);
                }
            });
        }

        // shutdown and wait for threads to catch up
        executor.shutdown();
        while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {}
    }

    private boolean clearConflictingGuids(final User user) {
        // make sure The Key guid doesn't conflict with an existing user
        int count = 0;
        final String guid = user.getTheKeyGuid();
        User existingUser;
        while ((existingUser = userManager.findUserByTheKeyGuid(guid)) != null) {
            try {
                userManager.generateNewGuid(existingUser);
            } catch (final UserException e) {
                LOG.debug("unable to generate new guid", e);
            }

            if (count++ > 5) {
                // short-circuit, but don't crash. failed migrations will be left in legacy ou
                return false;
            }
        }

        return true;
    }

    private boolean populateGuid(final User user) {
        // use key guid if it's not already in use
        final String guid = user.getTheKeyGuid();
        if (!(userManager.doesGuidExist(guid) || userManager.doesRelayGuidExist(guid))) {
            user.setGuid(guid);
            dao.updateGuid(user);
        }
        // otherwise generate a new guid
        else {
            try {
                userManager.generateNewGuid(user);
            } catch (final UserException e) {
                LOG.error("unable to generate new guid", e);
                return false;
            }
        }

        return true;
    }
}
