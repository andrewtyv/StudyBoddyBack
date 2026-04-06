package controllers;

import DTO.ApiResponse;
import DTO.ApiResponseWrapper;
import DTO.UserDTO;
import model.User;
import model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.JwtUtil;
import java.security.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller responsible for user authentication operations.
 *
 * <p>This controller provides endpoints for user registration and login.
 * During registration, a new user is created, the password is encoded,
 * and a JWT token is generated. During login, user credentials are checked
 * and a JWT token is returned if authentication is successful.</p>
 */
@RestController
@RequestMapping("/auth")
public class RegistrationController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    /**
     * Register a new user in the system.
     *
     * <p>This endpoint reads the username, email, and password from the request body.
     * It checks whether a user with the same email or username already exists.
     * If not, it creates a new user, encodes the password, saves the user
     * to the database, and generates a JWT token.</p>
     *
     * @param request map containing the registration data:
     *                {@code username}, {@code email}, {@code password}
     * @return an {@link ApiResponse} containing the result message and generated token,
     * if registration fails, the token is {@code null}
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account, saves the user in the database, and returns a JWT token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Registration successful. Please validate your email.",
                                              "data": "jwt_token_here"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User with this username or email already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "User with this username or email already exists",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/register")
    public ApiResponse registerUser(@RequestBody Map<String, String> request) {
        System.out.println("register");

        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password").trim();


        if (userRepo.existsByEmail(email) || userRepo.existsByUsername(username)) {
            return new ApiResponse("User with this username or email already exists ", null);
        }

        model.User user = new model.User(email, username, passwordEncoder.encode(password));
        user.setRole(UserRole.STUDENT);
        userRepo.save(user);

        String token = jwtUtil.generateToken(username);

        return new ApiResponse("Registration successful. Please validate your email.", token);
    }

    /**
     * Authenticates an existing user.
     *
     * <p>This endpoint reads the username and password from the request body,
     * finds the user by username, and compares the provided password with the
     * encoded password stored in the database. If the credentials are valid,
     * a JWT token is generated and returned.</p>
     *
     * @param request map containing the login data:
     *                {@code username} and {@code password}
     * @return an {@link ApiResponse} containing the result message and generated token,
     * if authentication fails, the token is {@code null}
     */
    @Operation(
            summary = "Login user",
            description = "Authenticates a user with username and password and returns a JWT token if credentials are valid."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Login succesfull",
                                              "data": "jwt_token_here"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid username or password",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "invalid login or password",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password").trim();

        model.User user = userRepo.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {            //String token = jwtUtil.generateToken(username);
            String token = jwtUtil.generateToken(username);
            return new ApiResponse("Login succesfull", token);
        }
        return new ApiResponse("invalid login or password", null);
    }

    @PostMapping("/google")
    public ApiResponseWrapper<UserDTO> googleLogin(@RequestBody Map<String, String> request) {
        String idTokenString = safeTrim(request.get("idToken"));

        if (idTokenString == null || idTokenString.isBlank()) {
            return ApiResponseWrapper.error("Google ID token is missing");
        }

        GoogleIdToken.Payload payload;
        try {
            payload = verifyGoogleIdToken(idTokenString);
        } catch (Exception e) {
            return ApiResponseWrapper.error("Failed to verify Google token");
        }

        if (payload == null) {
            return ApiResponseWrapper.error("Invalid Google token");
        }

        String googleSub = safeTrim(payload.getSubject());
        String email = normalizeEmail(payload.getEmail());
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = safeTrim((String) payload.get("name"));
        String picture = safeTrim((String) payload.get("picture"));

        if (googleSub == null || googleSub.isBlank() || email == null || email.isBlank()) {
            return ApiResponseWrapper.error("Google token does not contain required user data");
        }

        User user = userRepo.findByGoogleSub(googleSub).orElse(null);

        if (user == null) {
            User existingByEmail = userRepo.findByEmail(email);

            if (existingByEmail != null) {
                if (existingByEmail.getGoogleSub() != null
                        && !existingByEmail.getGoogleSub().equals(googleSub)) {
                    return ApiResponseWrapper.error("This email is already linked to another Google account");
                }

                user = existingByEmail;
                user.setGoogleSub(googleSub);
            } else {
                user = new User();
                user.setEmail(email);
                user.setUsername(generateUniqueUsername(name, email));
                user.setPassword(null);
                user.setStatus("ACTIVE");
                user.setEnabled(true);
                user.setRole(UserRole.STUDENT);
                user.setGoogleSub(googleSub);
                user.setCreatedAt(LocalDateTime.now());
            }
        }

        if ((user.getStatus() == null || user.getStatus().isBlank())
                || "PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
        }

        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }

        if (emailVerified && user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }

        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getUsername());

        return new ApiResponseWrapper<>(
                true,
                "Google login successful",
                toUserDTO(user),
                token
        );
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) throws Exception {
        GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
        return idToken != null ? idToken.getPayload() : null;
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO(user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                user.getEmailVerifiedAt(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getRole()
        );
        return dto;
    }

    private String generateUniqueUsername(String googleName, String email) {
        String raw = (googleName != null && !googleName.isBlank())
                ? googleName
                : email.substring(0, email.indexOf('@'));

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String base = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (base.isBlank()) {
            base = "user";
        }

        String candidate = base;
        int suffix = 1;

        while (userRepo.existsByUsername(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        String trimmed = safeTrim(email);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}



