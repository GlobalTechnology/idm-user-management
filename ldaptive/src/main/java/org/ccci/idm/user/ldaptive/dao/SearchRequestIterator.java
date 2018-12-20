package org.ccci.idm.user.ldaptive.dao;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import org.ccci.idm.user.dao.exception.InterruptedDaoException;
import org.ccci.idm.user.ldaptive.dao.exception.LdaptiveDaoException;
import org.ldaptive.Connection;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.control.PagedResultsControl;
import org.ldaptive.control.ResponseControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InterruptedNamingException;
import java.util.Collections;
import java.util.Iterator;

class SearchRequestIterator extends AbstractIterator<LdapEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchRequestIterator.class);

    @Nonnull
    private final SearchOperation searchOperation;
    @Nonnull
    private final SearchRequest searchRequest;
    @Nonnull
    private final PagedResultsControl pagedResultsControl;

    private byte[] cookie = null;
    @Nullable
    private Iterator<LdapEntry> currentPage = null;
    private boolean hasAnotherPage = true;

    SearchRequestIterator(@Nonnull final Connection connection, @Nonnull final SearchRequest request, int pageSize) {
        this(buildOperation(connection), request, pageSize);
    }

    @VisibleForTesting
    SearchRequestIterator(@Nonnull final SearchOperation operation, @Nonnull final SearchRequest request,
                          final int pageSize) {
        searchOperation = operation;
        searchRequest = request;
        pagedResultsControl = new PagedResultsControl(pageSize);
        searchRequest.setControls(pagedResultsControl);
    }

    @Override
    protected LdapEntry computeNext() {
        if (currentPage == null || !currentPage.hasNext()) {
            currentPage = loadNextPage();
        }

        if (!currentPage.hasNext()) {
            return endOfData();
        }

        return currentPage.next();
    }

    @Nonnull
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
            response = searchOperation.execute(searchRequest);
        } catch (LdapException e) {
            LOG.debug("error performing Ldap SearchRequest, wrapping & propagating exception", e);
            if (e.getCause() instanceof InterruptedNamingException) {
                throw new InterruptedDaoException(e);
            } else {
                throw new LdaptiveDaoException(e);
            }
        }

        // check the response to see if there is another page of results
        final ResponseControl pagedResults = response.getControl(PagedResultsControl.OID);
        if (pagedResults instanceof PagedResultsControl) {
            // get cookie for next page of results
            cookie = ((PagedResultsControl) pagedResults).getCookie();
        }
        hasAnotherPage = cookie != null && cookie.length > 0;

        // get an iterator for the current page
        return response.getResult().getEntries().iterator();
    }

    private static SearchOperation buildOperation(@Nonnull final Connection connection) {
        if (!connection.isOpen()) {
            throw new IllegalStateException("provided connection needs to already be open");
        }
        return new SearchOperation(connection);
    }
}
