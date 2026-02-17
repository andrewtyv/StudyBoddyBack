package controllers;

import DTO.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public class RegistrationController {


    @PostMapping("/register")
    public ApiResponse registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        //here will be added user to DB and then token should be generated


        return new ApiResponse("Registration successful. Please validate your email.", "jwt token will be here");
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        //check user credentials in BD -> generate JWT token


        return new ApiResponse("", "token if reg is succesfull");
    }

}
