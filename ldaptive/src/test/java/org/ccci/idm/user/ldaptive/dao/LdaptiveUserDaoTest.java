package org.ccci.idm.user.ldaptive.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.ccci.idm.user.dao.AbstractUserDaoTest;
import org.ccci.idm.user.ldaptive.dao.exception.LdaptiveDaoException;
import org.junit.Before;
import org.junit.Test;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchRequest;

import java.util.stream.Stream;

public class LdaptiveUserDaoTest extends AbstractUserDaoTest {
    private static final SearchRequest REQUEST = new SearchRequest();

    private LdaptiveUserDao dao;

    private ConnectionFactory connectionFactory;
    private Connection connection;

    @Before
    public void setup() throws Exception {
        connection = mock(Connection.class);
        connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.getConnection()).thenReturn(connection);

        dao = new LdaptiveUserDao();
        dao.setConnectionFactory(connectionFactory);
    }

    @Override
    protected LdaptiveUserDao getUserDao() {
        return dao;
    }

    @Test
    public void testCalculatePageSize() {
        dao.setMaxPageSize(10);
        dao.setMaxSearchResults(5);

        assertThat(dao.calculatePageSize(1, false), is(1));
        assertThat(dao.calculatePageSize(10, false), is(10));
        assertThat(dao.calculatePageSize(20, false), is(10));
        assertThat(dao.calculatePageSize(0, false), is(10));
        assertThat(dao.calculatePageSize(1, true), is(1));
        assertThat(dao.calculatePageSize(5, true), is(5));
        assertThat(dao.calculatePageSize(10, true), is(5 + 1));
        assertThat(dao.calculatePageSize(0, true), is(5 + 1));
    }

    @Test
    public void testStreamSearchRequestStreamClosesConnection() throws Exception {
        when(connection.isOpen()).thenReturn(true);

        try (Stream<LdapEntry> ignored = dao.streamSearchRequest(REQUEST, 1)) {
            verify(connection).open();
            verify(connection, never()).close();
        }
        verify(connection).close();
    }

    @Test
    public void testStreamSearchRequestCantOpenConnection() throws Exception {
        when(connection.open()).thenThrow(LdapException.class);

        try {
            dao.streamSearchRequest(REQUEST, 1);
            fail();
        } catch (LdaptiveDaoException expected) {}
        verify(connection).open();
    }
}
