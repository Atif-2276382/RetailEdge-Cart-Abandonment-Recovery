package com.retailedge.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtIdentity {

    private JwtIdentity() {
    }

    public static UUID requireOwnerId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException("Authenticated subject is required");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Authenticated subject must be a UUID", exception);
        }
    }
}