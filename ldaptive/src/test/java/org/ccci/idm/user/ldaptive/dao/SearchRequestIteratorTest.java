package org.ccci.idm.user.ldaptive.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.ccci.idm.user.dao.exception.InterruptedDaoException;
import org.ccci.idm.user.ldaptive.dao.exception.LdaptiveDaoException;
import org.junit.Before;
import org.junit.Test;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SortBehavior;
import org.ldaptive.control.PagedResultsControl;
import org.ldaptive.control.ResponseControl;
import org.mockito.stubbing.OngoingStubbing;

import javax.naming.InterruptedNamingException;
import java.util.NoSuchElementException;

public class SearchRequestIteratorTest {
    private final byte[] COOKIE = {0x1a};
    private final LdapEntry ENTRY1 = new LdapEntry("cn=entry1");
    private final LdapEntry ENTRY2 = new LdapEntry("cn=entry2");
    private final LdapEntry ENTRY3 = new LdapEntry("cn=entry3");

    private SearchOperation searchOperation;
    private SearchRequest searchRequest;
    private SearchRequestIterator iterator;

    @Before
    public void createIterator() {
        searchOperation = mock(SearchOperation.class);
        searchRequest = mock(SearchRequest.class);
        iterator = new SearchRequestIterator(searchOperation, searchRequest, 5);
    }

    @Test
    public void testNoResults() throws Exception {
        whenSearching().thenReturn(response());

        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) { }
        assertFalse(iterator.hasNext());
        verifyRawSearches(1);
    }

    @Test
    public void testSinglePage() throws Exception {
        whenSearching().thenReturn(response(ENTRY1));

        assertTrue(iterator.hasNext());
        assertEquals(ENTRY1, iterator.next());
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) { }
        verifyRawSearches(1);
    }

    @Test
    public void testMultiplePages() throws Exception {
        whenSearching().thenReturn(response(true, ENTRY1, ENTRY2)).thenReturn(response(ENTRY3));

        assertTrue(iterator.hasNext());
        assertEquals(ENTRY1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(ENTRY2, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(ENTRY3, iterator.next());
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) { }
        verifyRawSearches(2);
    }

    @Test
    public void testLdapException() throws Exception {
        whenSearching().thenThrow(LdapException.class);

        try {
            iterator.next();
            fail();
        } catch (LdaptiveDaoException expected) { }
        verifyRawSearches(1);
    }

    @Test
    public void testInterruptedLdapException() throws Exception {
        whenSearching().thenThrow(new LdapException(new InterruptedNamingException()));

        try {
            iterator.next();
            fail();
        } catch (InterruptedDaoException expected) { }
        verifyRawSearches(1);
    }

    private OngoingStubbing<Response<SearchResult>> whenSearching() throws Exception {
        return when(searchOperation.execute(searchRequest));
    }

    private Response<SearchResult> response(LdapEntry... entries) {
        return response(false, entries);
    }

    private Response<SearchResult> response(boolean hasMore, LdapEntry... entries) {
        final SearchResult result = new SearchResult(SortBehavior.ORDERED);
        result.addEntry(entries);
        final ResponseControl[] ctls = {new PagedResultsControl(5, hasMore ? COOKIE : null, false)};
        return new Response<>(result, ResultCode.SUCCESS, null, null, ctls, null, -1);
    }

    private void verifyRawSearches(final int count) throws Exception {
        verify(searchOperation, times(count)).execute(searchRequest);
    }
}
