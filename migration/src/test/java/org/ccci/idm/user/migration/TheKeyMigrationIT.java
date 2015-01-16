package org.ccci.idm.user.migration;

import static org.junit.Assume.assumeNotNull;

import com.google.common.base.Throwables;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.DaoException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        LOG.info("test start");

        // rotate guids while there are conflicting guids
        while (checkForConflictingGuids()) { }

        // fetch all remaining un-migrated Key users
        LOG.info("fetching legacy users");
        final List<User> users = userManager.getAllLegacyUsers(true);
        LOG.info("found {} users", users.size());

        // migrate users via thread pool
        final ExecutorService executor = Executors.newFixedThreadPool(50);
        for (final User user : users) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // clear any conflicting guids if possible
                    if (!clearConflictingGuids(user)) {
                        return;
                    }

                    // move user
                    if(!user.isDeactivated() && userManager.doesEmailExist(user.getEmail())) {
                        // deactivate user if there is a conflicting email address
                        dao.deactivateAndMoveLegacyKeyUser(user);
                    } else {
                        userManager.moveLegacyKeyUser(user);
                    }

                    // populate ccciGuid
                    populateGuid(user);
                }
            });
        }

        // shutdown and wait for threads to catch up
        executor.shutdown();
        while (!executor.awaitTermination(1, TimeUnit.SECONDS)) { }

        // set missing relayGuids
        this.updateRelayGuids();
    }

    private boolean checkForConflictingGuids() throws Exception {
        LOG.info("loading all users");
        final List<User> users = dao.findAll(true);
        LOG.info("found {} users", users.size());

        boolean changed = false;

        // check for conflicting relayGuids
        {
            final Map<String, User> relayGuids = new HashMap<String, User>(users.size());
            for (final User user : users) {
                if (relayGuids.containsKey(user.getRelayGuid())) {
                    LOG.info("conflicting relay guids");
                    if (user.getRawRelayGuid() == null) {
                        this.userManager.generateNewGuid(user);
                    } else {
                        final User user2 = relayGuids.remove(user.getRelayGuid());
                        this.userManager.generateNewGuid(user2);
                        relayGuids.put(user2.getRelayGuid(), user2);
                    }
                    changed = true;
                }
                relayGuids.put(user.getRelayGuid(), user);
            }
        }

        // check for conflicting thekeyGuids
        {
            final Map<String, User> thekeyGuids = new HashMap<String, User>(users.size());
            for (final User user : users) {
                if (thekeyGuids.containsKey(user.getTheKeyGuid())) {
                    LOG.info("conflicting Key guids");
                    if (user.getRawTheKeyGuid() == null) {
                        this.userManager.generateNewGuid(user);
                    } else {
                        final User user2 = thekeyGuids.remove(user.getTheKeyGuid());
                        this.userManager.generateNewGuid(user2);
                        thekeyGuids.put(user2.getTheKeyGuid(), user2);
                    }
                    changed = true;
                }
                thekeyGuids.put(user.getTheKeyGuid(), user);
            }
        }

        return changed;
    }

    private boolean clearConflictingGuids(final User user) {
        // make sure The Key guid doesn't conflict with an existing user
        int count = 0;
        final String guid = user.getTheKeyGuid();
        User existingUser;
        while ((existingUser = userManager.findUserByTheKeyGuid(guid)) != null) {
            try {
                if(guid.equals(existingUser.getRawTheKeyGuid())) {
                    LOG.error("this is strange, an account with the exact Key guid already exists");
                    return false;
                }

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

    private void updateRelayGuids() throws Exception {
        LOG.info("setting missing relayGuids");
        final List<User> users = dao.findAllMissingRelayGuid();
        LOG.info("found {} accounts to set relayGuid for", users.size());

        final ExecutorService executor = Executors.newFixedThreadPool(50);
        for (final User user : users) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    user.setRelayGuid(user.getRelayGuid());
                    try {
                        userManager.updateUser(user, User.Attr.RELAY_GUID);
                    } catch (final DaoException e) {
                        throw Throwables.propagate(e);
                    } catch (final UserException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        }

        executor.shutdown();
        while (!executor.awaitTermination(1, TimeUnit.SECONDS)) { ; }
    }
}
