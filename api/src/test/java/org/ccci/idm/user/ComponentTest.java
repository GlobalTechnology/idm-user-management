package org.ccci.idm.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ComponentTest {
    @Test
    public void verifyCompareToType() throws Exception {
        final Dn.Component component1 = new Dn.Component("cn", "Zebra");
        final Dn.Component component2 = new Dn.Component("ou", "Aardvark");

        assertThat(component1, comparesEqualTo(component1));
        assertThat(component2, comparesEqualTo(component2));
        assertThat(component1, is(greaterThan(component2)));
        assertThat(component2, is(lessThan(component1)));
    }

    @Test
    public void verifyCompareToValue() throws Exception {
        final Dn.Component component1 = new Dn.Component("cn", "Zebra");
        final Dn.Component component2 = new Dn.Component("ou", "Zebra");

        assertThat(component1, comparesEqualTo(component1));
        assertThat(component2, comparesEqualTo(component2));
        assertThat(component1, is(lessThan(component2)));
        assertThat(component2, is(greaterThan(component1)));
    }
}
