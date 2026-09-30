package com.example.invoiceapp.security;

import com.example.invoiceapp.model.enums.UserRole;

public final class AccessPolicy {
    private AccessPolicy() {}

    public static boolean canManageUsers(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN;
    }

    public static boolean canManageProducts(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN;
    }

    public static boolean canManageInventory(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.MANAGER;
    }

    public static boolean canViewInventory(UserRole role) {
        return role != null;
    }

    public static boolean canPurchases(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.MANAGER;
    }

    public static boolean canSales(UserRole role) {
        return role != null;
    }

    public static boolean canInvoices(UserRole role) {
        return role != null;
    }

    public static boolean canReports(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.MANAGER;
    }

    public static boolean canOrgSettings(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN;
    }

    public static void require(boolean allowed, String message) {
        if (!allowed) {
            throw new SecurityException(message);
        }
    }
}
