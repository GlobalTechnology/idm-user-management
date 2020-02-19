package org.ccci.idm.user.okta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OktaGroupTest {
    @Test
    fun testIsDescendantOfOrEqualToColon() {
        val group = OktaGroup(name = "Root:Branch")
        assertTrue(group.isDescendantOfOrEqualTo("Root"))
        assertTrue(group.isDescendantOfOrEqualTo("Root:Branch"))
        assertFalse(group.isDescendantOfOrEqualTo("Root:Branch:Leaf"))
    }

    @Test
    fun testIsDescendantOfOrEqualToDash() {
        val group = OktaGroup(name = "Root-Branch")
        assertTrue(group.isDescendantOfOrEqualTo("Root"))
        assertTrue(group.isDescendantOfOrEqualTo("Root-Branch"))
        assertFalse(group.isDescendantOfOrEqualTo("Root-Branch-Leaf"))
    }
}
