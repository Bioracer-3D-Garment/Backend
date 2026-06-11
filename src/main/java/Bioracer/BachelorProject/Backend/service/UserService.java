package Bioracer.BachelorProject.Backend.service;

import Bioracer.BachelorProject.Backend.controller.DTO.AuthenticationResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.UserInput;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.exception.UserException;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.repository.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public AuthenticationResponse authenticate(String email, String password) {
        try {
            final var usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(email, password);
            final var authentication = authenticationManager.authenticate(usernamePasswordAuthentication);
            final var user = ((UserDetailsImpl) authentication.getPrincipal()).user();
            final var token = jwtService.generateToken(user);
            return new AuthenticationResponse(
                    "Authentication successful.",
                    token,
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole());
        } catch (BadCredentialsException e) {
            throw new NotFoundException("Invalid email or password!");
        }
    }

    public AuthenticationResponse signup(UserInput userInput) {
        if (userRepository.existsByEmail(userInput.email())) {
            throw new UserException("Email is already in use!");
        }

        final var hashedPassword = passwordEncoder.encode(userInput.password());
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(userInput.role());
        final var user = new User(
                userInput.firstName(),
                userInput.lastName(),
                userInput.email(),
                hashedPassword,
                Role.USER);

        userRepository.save(user);

        final var usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(
                userInput.email(),
                userInput.password());
        final var authentication = authenticationManager.authenticate(usernamePasswordAuthentication);
        final var authUser = ((UserDetailsImpl) authentication.getPrincipal()).user();
        final var token = jwtService.generateToken(authUser);

        return new AuthenticationResponse(
                "User created and logged in successfully.",
                token,
                authUser.getEmail(),
                authUser.getFirstName(),
                authUser.getLastName(),
                authUser.getRole());
    }

}
