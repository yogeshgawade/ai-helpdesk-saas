package com.helpdesk.orgs;

public final class OrganizationContextHolder {

    private static final ThreadLocal<OrganizationContext> CONTEXT =
            new ThreadLocal<>();

    private OrganizationContextHolder() {
    }

    public static void set(OrganizationContext context) {
        CONTEXT.set(context);
    }

    public static OrganizationContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
