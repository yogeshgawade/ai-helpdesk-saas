package com.helpdesk.orgs;

import java.util.UUID;

public final class TenantDatabaseContextHolder {

    private static final ThreadLocal<UUID> CURRENT_ORGANIZATION =
            new ThreadLocal<>();

    private TenantDatabaseContextHolder() {
    }

    public static void set(UUID organizationId) {
        CURRENT_ORGANIZATION.set(organizationId);
    }

    public static UUID get() {
        return CURRENT_ORGANIZATION.get();
    }

    public static void clear() {
        CURRENT_ORGANIZATION.remove();
    }
}
