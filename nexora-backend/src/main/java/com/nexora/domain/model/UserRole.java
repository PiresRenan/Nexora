package com.nexora.domain.model;

/**
 * Hierarquia de papéis de usuário.
 * A ordem dos valores do enum define a hierarquia: maior ordinal = mais permissões.
 * Melhoria: método hasPermissionOf() encapsula lógica de autorização no domínio.
 */
public enum UserRole {
    CUSTOMER,
    SELLER,
    MANAGER,
    ADMIN;

    /**
     * Verifica se este papel possui as permissões do papel informado.
     * Ex: ADMIN.hasPermissionOf(SELLER) == true
     *     CUSTOMER.hasPermissionOf(ADMIN) == false
     */
    public boolean hasPermissionOf(UserRole requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }

    public boolean isEmployee() {
        return this != CUSTOMER;
    }

    public boolean isManager() {
        return this == MANAGER || this == ADMIN;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}