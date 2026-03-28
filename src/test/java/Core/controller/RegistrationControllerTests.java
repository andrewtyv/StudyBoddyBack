package Core.controller;


import DTO.ApiResponse;
import controllers.RegistrationController;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import repos.UserRepo;
import security.JwtUtil;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class RegistrationControllerTests {

    @InjectMocks
    private RegistrationController registrationController;

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private String email = "test@gmail.com";
    private String username = "test1";
    private String password = "testpasswd";

    @Test
    public void registerUser_success() {
        // Arrange
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", password
        );

        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPass");
        when(jwtUtil.generateToken(username)).thenReturn("mockedToken");

        // Act
        ApiResponse response = registrationController.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("Registration successful. Please validate your email.", response.getMessage());
        assertEquals("mockedToken", response.getToken());

        verify(userRepo).save(any(User.class));
    }
    @Test
    public void register_shouldReturnError_whenUserAlreadyExists() {
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", password
        );
        when(userRepo.existsByEmail(email)).thenReturn(true);


        //
        ApiResponse response = registrationController.registerUser(request);
        assertNotNull(response);
        assertEquals("User with this username or email already exists ", response.getMessage());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(anyString());


    }
}
