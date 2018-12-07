package org.ccci.idm.user.ldaptive.dao;

import org.ccci.idm.user.ldaptive.dao.exception.RuntimeLdaptiveException;
import org.ldaptive.Connection;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.control.PagedResultsControl;
import org.ldaptive.control.ResponseControl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;

public class SearchRequestIterator implements Iterator<LdapEntry> {
    @Nonnull
    private final Connection conn;
    @Nonnull
    private final SearchRequest searchRequest;
    @Nonnull
    private final PagedResultsControl pagedResultsControl;

    private SearchOperation search = null;
    byte[] cookie = null;
    @Nullable
    private Iterator<LdapEntry> currentPage = null;
    private boolean hasAnotherPage = true;

    public SearchRequestIterator(@Nonnull final Connection conn, @Nonnull final SearchRequest request) {
        this(conn, request, 100);
    }

    public SearchRequestIterator(@Nonnull final Connection connection, @Nonnull final SearchRequest request,
                                 int pageSize) {
        if (!connection.isOpen()) {
            throw new IllegalStateException("provided connection needs to already be open");
        }
        conn = connection;
        searchRequest = request;
        pagedResultsControl = new PagedResultsControl(pageSize);
        searchRequest.setControls(pagedResultsControl);
        init();
    }

    private void init() {
        search = new SearchOperation(conn);
    }

    @Override
    public boolean hasNext() {
        if (currentPage == null || !currentPage.hasNext()) {
            currentPage = loadNextPage();
        }

        return currentPage.hasNext();
    }

    @Override
    public LdapEntry next() {
        if (hasNext() && currentPage != null) {
            return currentPage.next();
        }
        return null;
    }

    private Iterator<LdapEntry> loadNextPage() {
        // short-circuit if we have reached the end of the results already
        if (!hasAnotherPage) {
            return Collections.emptyIterator();
        }

        // execute request
        pagedResultsControl.setCookie(cookie);
        cookie = null;
        final Response<SearchResult> response;
        try {
            response = search.execute(searchRequest);
        } catch (LdapException e) {
            throw new RuntimeLdaptiveException(e);
        }

        // process the PRC in the response
        final ResponseControl ctl = response.getControl(PagedResultsControl.OID);
        if (ctl instanceof PagedResultsControl) {
            // get cookie for next page of results
            cookie = ((PagedResultsControl) ctl).getCookie();
        }
        hasAnotherPage = cookie != null && cookie.length > 0;

        // get an iterator for the current page
        return response.getResult().getEntries().iterator();
    }
}
