package com.helpdesk.orgs;

import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizer;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class OrganizationTransactionConfig
        implements TransactionManagerCustomizer<JpaTransactionManager> {

    private final DataSource dataSource;

    public OrganizationTransactionConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(JpaTransactionManager transactionManager) {
        transactionManager.addListener(
                new TransactionExecutionListener() {

                    @Override
                    public void afterBegin(
                            TransactionExecution transaction,
                            Throwable beginFailure) {

                        if (beginFailure != null) {
                            return;
                        }

                        UUID organizationId =
                                TenantDatabaseContextHolder.get();

                        if (organizationId == null) {
                            return;
                        }

                        Connection connection =
                                DataSourceUtils.getConnection(dataSource);

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             "SELECT set_config(" +
                                                     "'app.current_org_id', " +
                                                     "?, true)"
                                     )) {

                            statement.setString(
                                    1,
                                    organizationId.toString()
                            );

                            statement.execute();
                        } catch (SQLException e) {
                            throw new IllegalStateException(
                                    "Failed to set PostgreSQL tenant context",
                                    e
                            );
                        }
                    }
                }
        );
    }
}
