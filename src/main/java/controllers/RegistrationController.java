package controllers;

import DTO.ApiResponse;
import model.UserRole;
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
     *         if registration fails, the token is {@code null}
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
            return new ApiResponse("User with this username or email already exists ",null);
        }

        model.User user = new model.User( email,username,passwordEncoder.encode(password));
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
     *         if authentication fails, the token is {@code null}
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

        model.User user =userRepo.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {            //String token = jwtUtil.generateToken(username);
            String token = jwtUtil.generateToken(username);
            return new ApiResponse("Login succesfull",token);
        }
        return new ApiResponse("invalid login or password", null);
    }

}
