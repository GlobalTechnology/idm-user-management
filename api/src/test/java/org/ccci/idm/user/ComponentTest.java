package org.ccci.idm.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ComponentTest {
    @Test
    public void verifyCompareToType() throws Exception {
        final Dn.Component component1 = new Dn.Component("1", "2");
        final Dn.Component component2 = new Dn.Component("2", "1");

        assertThat(component1.compareTo(component1), is(0));
        assertThat(component2.compareTo(component2), is(0));
        assertThat(component1.compareTo(component2), is(lessThan(0)));
        assertThat(component2.compareTo(component1), is(greaterThan(0)));
    }

    @Test
    public void verifyCompareToValue() throws Exception {
        final Dn.Component component1 = new Dn.Component("1", "1");
        final Dn.Component component2 = new Dn.Component("1", "2");

        assertThat(component1.compareTo(component1), is(0));
        assertThat(component2.compareTo(component2), is(0));
        assertThat(component1.compareTo(component2), is(lessThan(0)));
        assertThat(component2.compareTo(component1), is(greaterThan(0)));
    }
}
