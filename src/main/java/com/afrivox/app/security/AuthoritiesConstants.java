package com.afrivox.app.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String CONTRIBUTOR = "ROLE_CONTRIBUTOR";

    public static final String DEVELOPER = "ROLE_DEVELOPER";

    public static final String ORGANIZATION = "ROLE_ORGANIZATION";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {}
}
