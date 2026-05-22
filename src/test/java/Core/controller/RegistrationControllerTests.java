package Core.controller;

import DTO.ApiResponse;
import DTO.ApiResponseWrapper;
import DTO.UserDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import controllers.RegistrationController;
import model.User;
import model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.UserRepo;
import security.JwtUtil;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    private final String email = "test@gmail.com";
    private final String username = "test1";
    private final String password = "testpasswd";

    @Test
    public void registerUser_success() {
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", password
        );

        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPass");
        when(jwtUtil.generateToken(username)).thenReturn("mockedToken");

        ApiResponse response = registrationController.registerUser(request);

        assertNotNull(response);
        assertEquals("Registration successful. Please validate your email.", response.getMessage());
        assertEquals("mockedToken", response.getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(email, savedUser.getEmail());
        assertEquals(username, savedUser.getUsername());
        assertEquals("encodedPass", savedUser.getPassword());
        assertEquals(UserRole.STUDENT, savedUser.getRole());

        verify(passwordEncoder).encode(password);
        verify(jwtUtil).generateToken(username);
    }

    @Test
    public void registerUser_shouldReturnError_whenEmailAlreadyExists() {
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", password
        );

        when(userRepo.existsByEmail(email)).thenReturn(true);

        ApiResponse response = registrationController.registerUser(request);

        assertNotNull(response);
        assertEquals("User with this username or email already exists ", response.getMessage());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(anyString());

        // Через || existsByUsername не викликається, якщо email вже існує
        verify(userRepo, never()).existsByUsername(username);
    }

    @Test
    public void registerUser_shouldReturnError_whenUsernameAlreadyExists() {
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", password
        );

        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.existsByUsername(username)).thenReturn(true);

        ApiResponse response = registrationController.registerUser(request);

        assertNotNull(response);
        assertEquals("User with this username or email already exists ", response.getMessage());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void registerUser_shouldTrimPasswordBeforeEncoding() {
        Map<String, String> request = Map.of(
                "email", email,
                "username", username,
                "password", "   testpasswd   "
        );

        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPass");
        when(jwtUtil.generateToken(username)).thenReturn("mockedToken");

        ApiResponse response = registrationController.registerUser(request);

        assertNotNull(response);
        assertEquals("mockedToken", response.getToken());

        verify(passwordEncoder).encode("testpasswd");
        verify(userRepo).save(any(User.class));
    }

    @Test
    public void login_success() {
        Map<String, String> request = Map.of(
                "username", username,
                "password", password
        );

        User user = new User(email, username, "encodedPass");

        when(userRepo.findByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(password, "encodedPass")).thenReturn(true);
        when(jwtUtil.generateToken(username)).thenReturn("loginToken");

        ApiResponse response = registrationController.login(request);

        assertNotNull(response);
        assertEquals("Login succesfull", response.getMessage());
        assertEquals("loginToken", response.getToken());

        verify(userRepo).findByUsername(username);
        verify(passwordEncoder).matches(password, "encodedPass");
        verify(jwtUtil).generateToken(username);
    }

    @Test
    public void login_shouldReturnError_whenUserNotFound() {
        Map<String, String> request = Map.of(
                "username", username,
                "password", password
        );

        when(userRepo.findByUsername(username)).thenReturn(null);

        ApiResponse response = registrationController.login(request);

        assertNotNull(response);
        assertEquals("invalid login or password", response.getMessage());
        assertNull(response.getToken());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void login_shouldReturnError_whenPasswordIsWrong() {
        Map<String, String> request = Map.of(
                "username", username,
                "password", password
        );

        User user = new User(email, username, "encodedPass");

        when(userRepo.findByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(password, "encodedPass")).thenReturn(false);

        ApiResponse response = registrationController.login(request);

        assertNotNull(response);
        assertEquals("invalid login or password", response.getMessage());
        assertNull(response.getToken());

        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void login_shouldTrimPasswordBeforeChecking() {
        Map<String, String> request = Map.of(
                "username", username,
                "password", "   testpasswd   "
        );

        User user = new User(email, username, "encodedPass");

        when(userRepo.findByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(password, "encodedPass")).thenReturn(true);
        when(jwtUtil.generateToken(username)).thenReturn("loginToken");

        ApiResponse response = registrationController.login(request);

        assertNotNull(response);
        assertEquals("Login succesfull", response.getMessage());
        assertEquals("loginToken", response.getToken());

        verify(passwordEncoder).matches("testpasswd", "encodedPass");
    }


    @Test
    public void googleLogin_shouldReturnError_whenIdTokenIsMissing() {
        Map<String, String> request = Map.of(
                "idToken", "   "
        );

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Google ID token is missing", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getToken());

        verifyNoInteractions(googleIdTokenVerifier);
        verify(userRepo, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void googleLogin_shouldReturnError_whenVerifierThrowsException() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "bad-token"
        );

        when(googleIdTokenVerifier.verify("bad-token"))
                .thenThrow(new RuntimeException("Google verification failed"));

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Failed to verify Google token", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void googleLogin_shouldReturnError_whenGoogleTokenIsInvalid() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "invalid-token"
        );

        when(googleIdTokenVerifier.verify("invalid-token")).thenReturn(null);

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Invalid Google token", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void googleLogin_shouldReturnError_whenPayloadDoesNotContainRequiredData() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                null,
                "test@gmail.com",
                true,
                "Test User",
                "https://picture.com/avatar.png"
        );

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Google token does not contain required user data", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void googleLogin_success_shouldCreateNewUser_whenUserDoesNotExist() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "google-sub-123",
                "John.Doe@GMAIL.COM",
                true,
                "John Doe",
                "https://picture.com/avatar.png"
        );

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("john.doe@gmail.com")).thenReturn(null);
        when(userRepo.existsByUsername("john_doe")).thenReturn(false);
        when(jwtUtil.generateToken("john_doe")).thenReturn("googleJwtToken");

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Google login successful", response.getMessage());
        assertEquals("googleJwtToken", response.getToken());
        assertNotNull(response.getData());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("john.doe@gmail.com", savedUser.getEmail());
        assertEquals("john_doe", savedUser.getUsername());
        assertNull(savedUser.getPassword());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertTrue(savedUser.getEnabled());
        assertEquals(UserRole.STUDENT, savedUser.getRole());
        assertEquals("google-sub-123", savedUser.getGoogleSub());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getEmailVerifiedAt());

        verify(jwtUtil).generateToken("john_doe");
    }

    @Test
    public void googleLogin_success_shouldCreateUniqueUsernameWithSuffix_whenUsernameAlreadyExists() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "google-sub-123",
                "john@gmail.com",
                true,
                "John Doe",
                null
        );

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("john@gmail.com")).thenReturn(null);
        when(userRepo.existsByUsername("john_doe")).thenReturn(true);
        when(userRepo.existsByUsername("john_doe_1")).thenReturn(false);
        when(jwtUtil.generateToken("john_doe_1")).thenReturn("googleJwtToken");

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("googleJwtToken", response.getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("john_doe_1", savedUser.getUsername());

        verify(jwtUtil).generateToken("john_doe_1");
    }

    @Test
    public void googleLogin_success_shouldUseEmailPrefix_whenGoogleNameIsMissing() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "google-sub-123",
                "student@gmail.com",
                true,
                "   ",
                null
        );

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("student@gmail.com")).thenReturn(null);
        when(userRepo.existsByUsername("student")).thenReturn(false);
        when(jwtUtil.generateToken("student")).thenReturn("googleJwtToken");

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("googleJwtToken", response.getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("student", savedUser.getUsername());
    }

    @Test
    public void googleLogin_success_shouldLinkExistingUserByEmail_whenGoogleSubIsNull() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "google-sub-123",
                "existing@gmail.com",
                true,
                "Existing User",
                null
        );

        User existingUser = new User();
        existingUser.setEmail("existing@gmail.com");
        existingUser.setUsername("existing_user");
        existingUser.setPassword("encodedPassword");
        existingUser.setStatus("PENDING_VERIFICATION");
        existingUser.setEnabled(null);
        existingUser.setGoogleSub(null);

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("existing@gmail.com")).thenReturn(existingUser);
        when(jwtUtil.generateToken("existing_user")).thenReturn("linkedToken");

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Google login successful", response.getMessage());
        assertEquals("linkedToken", response.getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("google-sub-123", savedUser.getGoogleSub());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertTrue(savedUser.getEnabled());
        assertNotNull(savedUser.getEmailVerifiedAt());

        verify(jwtUtil).generateToken("existing_user");
    }

    @Test
    public void googleLogin_shouldReturnError_whenEmailLinkedToAnotherGoogleAccount() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "new-google-sub",
                "existing@gmail.com",
                true,
                "Existing User",
                null
        );

        User existingUser = new User();
        existingUser.setEmail("existing@gmail.com");
        existingUser.setUsername("existing_user");
        existingUser.setGoogleSub("another-google-sub");

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("new-google-sub")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("existing@gmail.com")).thenReturn(existingUser);

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("This email is already linked to another Google account", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getToken());

        verify(userRepo, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    public void googleLogin_success_shouldUseExistingUserFoundByGoogleSub() throws Exception {
        Map<String, String> request = Map.of(
                "idToken", "valid-token"
        );

        GoogleIdToken idToken = buildGoogleIdToken(
                "google-sub-123",
                "existing@gmail.com",
                true,
                "Existing User",
                null
        );

        User existingUser = new User();
        existingUser.setEmail("existing@gmail.com");
        existingUser.setUsername("existing_user");
        existingUser.setGoogleSub("google-sub-123");
        existingUser.setStatus("PENDING_VERIFICATION");
        existingUser.setEnabled(null);
        existingUser.setEmailVerifiedAt(null);

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(userRepo.findByGoogleSub("google-sub-123")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken("existing_user")).thenReturn("existingGoogleToken");

        ApiResponseWrapper<UserDTO> response = registrationController.googleLogin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Google login successful", response.getMessage());
        assertEquals("existingGoogleToken", response.getToken());
        assertNotNull(response.getData());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("existing_user", savedUser.getUsername());
        assertEquals("google-sub-123", savedUser.getGoogleSub());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertTrue(savedUser.getEnabled());
        assertNotNull(savedUser.getEmailVerifiedAt());

        verify(userRepo, never()).findByEmail(anyString());
        verify(jwtUtil).generateToken("existing_user");
    }

    private GoogleIdToken buildGoogleIdToken(
            String subject,
            String email,
            Boolean emailVerified,
            String name,
            String picture
    ) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();

        payload.setSubject(subject);
        payload.setEmail(email);
        payload.setEmailVerified(emailVerified);

        if (name != null) {
            payload.set("name", name);
        }

        if (picture != null) {
            payload.set("picture", picture);
        }

        JsonWebSignature.Header header = new JsonWebSignature.Header();

        return new GoogleIdToken(
                header,
                payload,
                new byte[0],
                new byte[0]
        );
    }
}