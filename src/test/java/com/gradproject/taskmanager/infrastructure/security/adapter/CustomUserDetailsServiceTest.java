package com.gradproject.taskmanager.infrastructure.security.adapter;

import com.gradproject.taskmanager.modules.auth.domain.Role;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
        
        String username = "testuser";
        User user = createTestUser(username, "password123", true, false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo("password123");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionWhenUserNotFound() {
        
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + username);

        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_shouldMapUserRolesToAuthorities() {
        
        String username = "testuser";
        User user = createTestUser(username, "password", true, false);

        Role role1 = new Role();
        role1.setName("ROLE_USER");

        Role role2 = new Role();
        role2.setName("ROLE_ADMIN");

        user.setRoles(Set.of(role1, role2));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_shouldHandleEmptyRoles() {
        
        String username = "testuser";
        User user = createTestUser(username, "password", true, false);
        user.setRoles(Set.of());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_shouldHandleLockedAccount() {
        
        String username = "lockeduser";
        User user = createTestUser(username, "password", true, true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_shouldHandleDisabledAccount() {
        
        String username = "disableduser";
        User user = createTestUser(username, "password", false, false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_shouldHandleDisabledAndLockedAccount() {
        
        String username = "blockeduser";
        User user = createTestUser(username, "password", false, true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_shouldMapPasswordCorrectly() {
        
        String username = "testuser";
        String encodedPassword = "$2a$10$encodedPasswordHashHere";
        User user = createTestUser(username, encodedPassword, true, false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    void loadUserByUsername_shouldHandleSingleRole() {
        
        String username = "testuser";
        User user = createTestUser(username, "password", true, false);

        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_shouldReturnAccountNonExpiredAsTrue() {
        
        String username = "testuser";
        User user = createTestUser(username, "password", true, false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_shouldReturnCredentialsNonExpiredAsTrue() {
        
        String username = "testuser";
        User user = createTestUser(username, "password", true, false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    private User createTestUser(String username, String password, boolean enabled, boolean locked) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(username + "@example.com");
        user.setEnabled(enabled);
        user.setLocked(locked);

        Role defaultRole = new Role();
        defaultRole.setName("ROLE_USER");
        user.setRoles(Set.of(defaultRole));

        return user;
    }
}
