package com.example.usermanagement.enums;

public enum KeycloakRole {
    ADMIN(true, true),
    OPERATOR(false, true),
    USER(false, false);

    private final boolean canSeeTaxCode;
    private final boolean canSeeRoles;

    KeycloakRole(boolean canSeeTaxCode, boolean canSeeRoles) {
        this.canSeeTaxCode = canSeeTaxCode;
        this.canSeeRoles = canSeeRoles;
    }

    public boolean canSeeTaxCode() {
        return canSeeTaxCode;
    }

    public boolean canSeeRoles() {
        return canSeeRoles;
    }
}
