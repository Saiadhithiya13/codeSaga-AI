package com.codesage.security.principal;

import com.codesage.domain.auth.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal backed by a CodeSage {@link User}.
 *
 * <p>Implements {@link UserDetails} so Spring Security can use it
 * in the {@code SecurityContext}. The {@link com.codesage.security.jwt.JwtAuthenticationFilter}
 * constructs this from JWT claims directly — <strong>no database call per request</strong>.
 *
 * <p>For endpoints that need full user data (e.g. {@code GET /api/v1/users/me}),
 * the controller calls {@link com.codesage.domain.auth.service.UserService} explicitly.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String login;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructs a principal from JWT claims (no DB lookup required).
     * Used by {@link com.codesage.security.jwt.JwtAuthenticationFilter}.
     */
    public UserPrincipal(UUID id, String login, String email, String role) {
        this.id          = id;
        this.login       = login;
        this.email       = email;
        this.role        = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * Constructs a principal from a fully loaded {@link User} entity.
     * Used when a DB-backed principal is needed.
     */
    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getLogin(),
                user.getEmail(),
                user.getRole()
        );
    }

    // ─── UserDetails ─────────────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return login;
    }

    /**
     * Password is not used — authentication is via GitHub OAuth + JWT only.
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
