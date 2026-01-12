package dev.ctlabs.starter.auth.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("ROLE_ADMIN"),
    BUSINESS("ROLE_BUSINESS"),
    SELLER("ROLE_SELLER"),
    CUSTOMER("ROLE_CUSTOMER");

    private final String authority;
}