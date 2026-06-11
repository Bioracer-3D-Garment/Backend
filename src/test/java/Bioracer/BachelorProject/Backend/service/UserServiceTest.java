package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import Bioracer.BachelorProject.Backend.controller.DTO.AuthenticationResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.UserInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Jane", "Doe", "jane@example.com", "hashed", Role.USER);
    }

    private Authentication authenticationFor(User principalUser) {
        return new UsernamePasswordAuthenticationToken(new UserDetailsImpl(principalUser), null, List.of());
    }

    @Test
    void getAllUsersReturnsAllUsers() {
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);

        assertThat(userService.getAllUsers()).containsExactlyElementsOf(users);
    }

    @Test
    void authenticateReturnsTokenAndUserDetails() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticationFor(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = userService.authenticate("jane@example.com", "secret");

        assertThat(response.message()).isEqualTo("Authentication successful.");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void authenticateThrowsWhenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.authenticate("jane@example.com", "wrong"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Invalid email or password!");
    }

    @Test
    void signupHashesPasswordAndStoresUserWithUserRole() {
        UserInput input = new UserInput("secret", "Jane", "Doe", "jane@example.com", "admin");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticationFor(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = userService.signup(input);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo("Jane");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");
        assertThat(savedUser.getEmail()).isEqualTo("jane@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("hashed");
        // role from input is ignored: new signups always get Role.USER
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        assertThat(response.message()).isEqualTo("User created and logged in successfully.");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    void signupThrowsWhenEmailIsAlreadyInUse() {
        UserInput input = new UserInput("secret", "Jane", "Doe", "jane@example.com", "user");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(input))
                .isInstanceOf(UserException.class)
                .hasMessage("Email is already in use!");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsersReturnsEmptyListWhenThereAreNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    @Test
    void authenticatePassesCredentialsToAuthenticationManager() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticationFor(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        userService.authenticate("jane@example.com", "secret");

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        assertThat(authenticationCaptor.getValue().getName()).isEqualTo("jane@example.com");
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("secret");
    }

    @Test
    void authenticatePropagatesUnexpectedErrors() {
        // only BadCredentialsException is translated; other failures must surface as-is
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new IllegalStateException("authentication service unavailable"));

        assertThatThrownBy(() -> userService.authenticate("jane@example.com", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("authentication service unavailable");
    }

    @Test
    void signupAuthenticatesWithTheSignupCredentials() {
        UserInput input = new UserInput("secret", "Jane", "Doe", "jane@example.com", "user");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticationFor(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        userService.signup(input);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        assertThat(authenticationCaptor.getValue().getName()).isEqualTo("jane@example.com");
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("secret");
    }
}
