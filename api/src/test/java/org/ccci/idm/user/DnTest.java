package org.ccci.idm.user;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DnTest {
    private final static Dn ROOT = new Dn(new Dn.Component("dc", "org"), new Dn.Component("dc", "ccci"));
    private final static Dn GROUP = ROOT.descendant(new Dn.Component("ou", "group"));

    @Test
    public void verifyEqualsCaseInsensitive() throws Exception {
        final Dn dn1 = new Dn(new Dn.Component("ou", "OuTeR"), new Dn.Component("cn", "inner"));
        final Dn dn2 = new Dn(new Dn.Component("OU", "outer"), new Dn.Component("CN", "INNER"));

        assertThat(dn1, is(dn2));
        assertThat(dn1.hashCode(), is(dn2.hashCode()));
    }

    @Test
    public void verifyParent() throws Exception {
        assertNotEquals(ROOT, GROUP);
        assertEquals(ROOT, GROUP.parent());

        assertNull(new Dn().parent());
    }

    @Test
    public void verifyIsAncestorOf() {
        // normal usages
        assertThat(ROOT.isAncestorOf(GROUP), is(true));
        assertThat(GROUP.isAncestorOf(ROOT), is(false));

        // edge case, you can't be you own ancestor unlike Fry on Futurama
        assertThat(ROOT.isAncestorOf(ROOT), is(false));
    }

    @Test
    public void verifyIsDescendantOf() {
        // normal usages
        assertThat(GROUP.isDescendantOf(ROOT), is(true));
        assertThat(ROOT.isDescendantOf(GROUP), is(false));

        // edge case, you can't be you own descendant
        assertThat(ROOT.isDescendantOf(ROOT), is(false));
    }
}
