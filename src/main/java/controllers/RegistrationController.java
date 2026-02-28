package controllers;

import DTO.ApiResponse;
import model.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/auth")
public class RegistrationController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;



   // @Autowired
    //private JwtUtil jwtUtil;


    @PostMapping("/register")
    public ApiResponse registerUser(@RequestBody Map<String, String> request) {
        System.out.println("register");

        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");


        if (userRepo.existsByEmail(email) || userRepo.existsByUsername(username)) {
            return new ApiResponse("User with this username and password already exists ",null);
        }

        model.User user = new model.User( email,username,passwordEncoder.encode(password));

        userRepo.save(user);

        //String token = jwtUtil.generateToken(username);

        return new ApiResponse("Registration successful. Please validate your email.", "token");
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String encodedPassword = passwordEncoder.encode(password);

        model.User user =userRepo.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {            //String token = jwtUtil.generateToken(username);
            return new ApiResponse("Login succesfull","token");
        }
        return new ApiResponse("invalid email or password", null);
    }

}
