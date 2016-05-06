package org.ccci.idm.user;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DnTest {
    private final static Dn ROOT = new Dn(new Dn.Component("dc", "org"), new Dn.Component("dc", "ccci"));
    private final static Dn CHILD1 = ROOT.descendant(new Dn.Component("ou", "child1"));
    private final static Dn CHILD2 = ROOT.descendant(new Dn.Component("ou", "child2"));

    @Test
    public void verifyEqualsCaseInsensitive() throws Exception {
        final Dn dn1 = new Dn(new Dn.Component("ou", "OuTeR"), new Dn.Component("cn", "inner"));
        final Dn dn2 = new Dn(new Dn.Component("OU", "outer"), new Dn.Component("CN", "INNER"));

        assertThat(dn1, is(dn2));
        assertThat(dn1.hashCode(), is(dn2.hashCode()));
    }

    @Test
    public void verifyParent() throws Exception {
        assertNotEquals(ROOT, CHILD1);
        assertEquals(ROOT, CHILD1.parent());
        assertEquals(ROOT, CHILD2.parent());
        assertThat(CHILD1.parent(), is(CHILD2.parent()));

        assertNull(Dn.ROOT.parent());
    }

    @Test
    public void verifyIsAncestorOf() throws Exception {
        // normal usages
        assertThat(ROOT.isAncestorOf(CHILD1), is(true));
        assertThat(ROOT.isAncestorOf(CHILD2), is(true));
        assertThat(CHILD1.isAncestorOf(ROOT), is(false));
        assertThat(CHILD2.isAncestorOf(ROOT), is(false));
        assertThat(CHILD1.isAncestorOf(CHILD2), is(false));
        assertThat(CHILD2.isAncestorOf(CHILD1), is(false));

        // edge case, you can't be you own ancestor unlike Fry on Futurama
        assertThat(ROOT.isAncestorOf(ROOT), is(false));
    }

    @Test
    public void verifyIsAncestorOfOrEqualTo() throws Exception {
        // normal usages
        assertThat(ROOT.isAncestorOfOrEqualTo(ROOT), is(true));
        assertThat(ROOT.isAncestorOfOrEqualTo(CHILD1), is(true));
        assertThat(ROOT.isAncestorOfOrEqualTo(CHILD2), is(true));
        assertThat(CHILD1.isAncestorOfOrEqualTo(ROOT), is(false));
        assertThat(CHILD2.isAncestorOfOrEqualTo(ROOT), is(false));
        assertThat(CHILD1.isAncestorOfOrEqualTo(CHILD2), is(false));
        assertThat(CHILD2.isAncestorOfOrEqualTo(CHILD1), is(false));
    }

    @Test
    public void verifyIsDescendantOf() throws Exception {
        // normal usages
        assertThat(CHILD1.isDescendantOf(ROOT), is(true));
        assertThat(CHILD2.isDescendantOf(ROOT), is(true));
        assertThat(ROOT.isDescendantOf(CHILD1), is(false));
        assertThat(ROOT.isDescendantOf(CHILD2), is(false));
        assertThat(CHILD1.isDescendantOf(CHILD2), is(false));
        assertThat(CHILD2.isDescendantOf(CHILD1), is(false));

        // edge case, you can't be you own descendant
        assertThat(ROOT.isDescendantOf(ROOT), is(false));
    }

    @Test
    public void verifyIsDescendantOfOrEqualTo() throws Exception {
        // normal usages
        assertThat(ROOT.isDescendantOfOrEqualTo(ROOT), is(true));
        assertThat(CHILD1.isDescendantOfOrEqualTo(ROOT), is(true));
        assertThat(CHILD2.isDescendantOfOrEqualTo(ROOT), is(true));
        assertThat(ROOT.isDescendantOfOrEqualTo(CHILD1), is(false));
        assertThat(ROOT.isDescendantOfOrEqualTo(CHILD2), is(false));
        assertThat(CHILD1.isDescendantOfOrEqualTo(CHILD2), is(false));
        assertThat(CHILD2.isDescendantOfOrEqualTo(CHILD1), is(false));
    }
}
