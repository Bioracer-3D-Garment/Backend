package Bioracer.BachelorProject.Backend.controller;

import Bioracer.BachelorProject.Backend.controller.DTO.AuthenticationRequest;
import Bioracer.BachelorProject.Backend.controller.DTO.AuthenticationResponse;
import Bioracer.BachelorProject.Backend.controller.DTO.UserInput;
import Bioracer.BachelorProject.Backend.model.User;
import Bioracer.BachelorProject.Backend.service.UserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody AuthenticationRequest authenticationRequest) {
        return userService.authenticate(authenticationRequest.email(), authenticationRequest.password());
    }

    @PostMapping("/signup")
    public AuthenticationResponse signup(@Valid @RequestBody UserInput userInput) {
        return userService.signup(userInput);
    }
}
