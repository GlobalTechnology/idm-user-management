package org.ccci.idm.user.ldaptive.dao.util;

import org.ccci.idm.user.Group;
import org.ldaptive.io.ValueTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * author@lee.braddock
 */
public class GroupUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GroupUtils.class);

    public static Group fromDn(String dn, ValueTranscoder<Group> valueTranscoder) {
        try {
            return valueTranscoder != null ? valueTranscoder.decodeStringValue(dn) : null;
        } catch (final Exception e) {
            LOG.debug("Caught exception resolving group from group dn {}", dn);
            return null;
        }
    }
}
