package com.lms.mentoring.unit.multitenancy;

import com.lms.mentoring.multitenancy.TenantSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantSchemaService}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TenantSchemaServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private TenantSchemaService tenantSchemaService;

    @BeforeEach
    void setUp() {
        tenantSchemaService = new TenantSchemaService(dataSource);
    }

    @Test
    void createTenantSchema_WithProviderTenant_ShouldNotCreateSchema() {
        // given - provider tenant returns null schema name

        // when
        tenantSchemaService.createTenantSchema("PROVIDER");

        // then - no database operations should occur
        verifyNoInteractions(dataSource);
    }

    @Test
    void dropTenantSchema_WithProviderTenant_ShouldNotDropSchema() {
        // given - provider tenant returns null schema name

        // when
        tenantSchemaService.dropTenantSchema("PROVIDER");

        // then - no database operations should occur
        verifyNoInteractions(dataSource);
    }

    @Test
    void dropTenantSchema_WhenSchemaExists_ShouldDropSchema() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        // when
        tenantSchemaService.dropTenantSchema(tenantId);

        // then
        verify(statement).execute(contains("DROP SCHEMA"));
        verify(connection).close();
    }

    @Test
    void dropTenantSchema_WhenSqlExceptionOccurs_ShouldLogWarningAndNotFail() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("Schema not found"));

        // when - should not throw
        assertDoesNotThrow(() -> tenantSchemaService.dropTenantSchema(tenantId));

        // then
        verify(connection).close();
    }

    @Test
    void schemaExists_WithProviderTenant_ShouldReturnTrue() {
        // given - provider tenant returns null schema name

        // when
        boolean exists = tenantSchemaService.schemaExists("PROVIDER");

        // then
        assertTrue(exists);
        verifyNoInteractions(dataSource);
    }

    @Test
    void schemaExists_WhenSchemaExistsInH2_ShouldReturnTrue() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        
        // First query (HANA) fails, second (H2 INFORMATION_SCHEMA) succeeds
        when(statement.executeQuery(contains("SYS.SCHEMAS")))
            .thenThrow(new SQLException("Table not found"));
        when(statement.executeQuery(contains("INFORMATION_SCHEMA")))
            .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        // when
        boolean exists = tenantSchemaService.schemaExists(tenantId);

        // then
        assertTrue(exists);
    }

    @Test
    void schemaExists_WhenSchemaDoesNotExist_ShouldReturnFalse() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(contains("SYS.SCHEMAS")))
            .thenThrow(new SQLException("Table not found"));
        when(statement.executeQuery(contains("INFORMATION_SCHEMA")))
            .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // when
        boolean exists = tenantSchemaService.schemaExists(tenantId);

        // then
        assertFalse(exists);
    }

    @Test
    void schemaExists_WhenSqlExceptionOccurs_ShouldReturnFalse() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when
        boolean exists = tenantSchemaService.schemaExists(tenantId);

        // then
        assertFalse(exists);
    }

    @Test
    void schemaExists_WhenHanaSyntaxWorks_ShouldReturnTrue() throws SQLException {
        // given
        String tenantId = "tenant-12345678-abcd";
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(contains("SYS.SCHEMAS"))).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        // when
        boolean exists = tenantSchemaService.schemaExists(tenantId);

        // then
        assertTrue(exists);
    }
}