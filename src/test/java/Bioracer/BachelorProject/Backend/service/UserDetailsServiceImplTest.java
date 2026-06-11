package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER);
    }

    @Test
    void loadUserByUsernameReturnsUserDetails() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("jane@example.com");

        assertThat(details.getUsername()).isEqualTo("jane@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("unknown@example.com");
    }

    @Test
    void updatePasswordEncodesAndSavesNewPassword() {
        when(passwordEncoder.encode("newSecret")).thenReturn("newHashed");
        when(userRepository.save(user)).thenReturn(user);

        UserDetails updated = userDetailsService.updatePassword(new UserDetailsImpl(user), "newSecret");

        assertThat(user.getPassword()).isEqualTo("newHashed");
        assertThat(updated.getPassword()).isEqualTo("newHashed");
    }
}
