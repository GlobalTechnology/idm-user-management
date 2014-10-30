package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.AbstractDefaultUserManagerIT;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/org/ccci/idm/user/usermanager.xml", "ldap.xml", "config.xml", "dao-default.xml"})
public class DefaultUserManagerIT extends AbstractDefaultUserManagerIT {

}
