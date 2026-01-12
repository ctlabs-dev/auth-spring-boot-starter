package dev.ctlabs.starter.auth.infrastructure.security;

import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Optional<User> userOptional = Optional.empty();
        boolean isEmailLogin = false;

        if (username.contains("@")) {
            userOptional = userRepository.findByEmail(username);
            isEmailLogin = true;
        } else if (username.matches("^\\+[1-9]\\d{1,14}$")) {
            userOptional = userRepository.findByPhoneNumber(username);
        }

        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        boolean isVerified = false;
        if (user.getVerification() != null) {
            isVerified = isEmailLogin ? user.getVerification().isEmailVerified() : user.getVerification().isPhoneVerified();
        }

        String principal = user.getEmail() != null ? user.getEmail() : user.getPhoneNumber();

        return new org.springframework.security.core.userdetails.User(
                principal,
                user.getPassword(),
                isVerified,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getAuthority()))
        );
    }
}
