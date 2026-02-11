package dev.ctlabs.starter.auth.infrastructure.security;

import dev.ctlabs.starter.auth.domain.model.Permission;
import dev.ctlabs.starter.auth.domain.model.Role;
import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = Optional.empty();
        boolean isEmailLogin = false;

        if (username.contains("@")) {
            userOptional = userRepository.findByEmail(username);
            isEmailLogin = true;
        } else {
            userOptional = userRepository.findByPhoneNumber(username);
        }

        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        boolean isVerified = false;
        if (isEmailLogin) {
            isVerified = user.isEmailVerified();
        } else {
            isVerified = user.isPhoneVerified();
        }

        boolean isActive = "active".equalsIgnoreCase(user.getStatus());
        boolean isAccountNonLocked = !"suspended".equalsIgnoreCase(user.getStatus()) && !"banned".equalsIgnoreCase(user.getStatus());

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getSlug()));
            }
        }

        String principal = user.getEmail() != null ? user.getEmail() : user.getPhoneNumber();

        return new org.springframework.security.core.userdetails.User(
                principal,
                user.getPassword() == null ? "" : user.getPassword(),
                isVerified && isActive,
                true,
                true,
                isAccountNonLocked,
                authorities
        );
    }
}
