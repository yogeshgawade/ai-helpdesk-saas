package com.helpdesk.orgs;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class TenantTransactionExecutor {

    private final TransactionTemplate transactionTemplate;

    public TenantTransactionExecutor(
            PlatformTransactionManager transactionManager) {

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    public <T> T execute(
            UUID organizationId,
            Supplier<T> operation) {

        if (organizationId == null) {
            throw new IllegalArgumentException(
                    "organizationId must not be null"
            );
        }

        UUID previousOrganizationId =
                TenantDatabaseContextHolder.get();

        TenantDatabaseContextHolder.set(organizationId);

        try {
            return transactionTemplate.execute(
                    status -> operation.get()
            );
        } finally {
            if (previousOrganizationId == null) {
                TenantDatabaseContextHolder.clear();
            } else {
                TenantDatabaseContextHolder.set(
                        previousOrganizationId
                );
            }
        }
    }

    public void execute(
            UUID organizationId,
            Runnable operation) {

        execute(
                organizationId,
                () -> {
                    operation.run();
                    return null;
                }
        );
    }
}
