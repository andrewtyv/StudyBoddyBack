package controllers;

import DTO.ApiResponseWrapper;
import DTO.UserDTO;
import model.User;
import model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import repos.UserRepo;

import java.security.Principal;

@RestController
@RequestMapping("/user")
public class SettingsController {

    @Autowired
    UserRepo userRepo;

    @GetMapping("/me")
    public ApiResponseWrapper<UserDTO> getMe(Principal principal) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        return ApiResponseWrapper.ok(
                new UserDTO(
                        me.getId(),
                        me.getEmail(),
                        me.getUsername(),
                        null,
                        me.getStatus(),
                        me.getEmailVerifiedAt(),
                        me.getEnabled(),
                        me.getCreatedAt(),
                        me.getRole(),
                        me.getInstitute(),
                        me.getFaculty(),
                        me.getSubjects()
                )
        );
    }

    @PutMapping("/role")
    public ApiResponseWrapper<UserDTO> updateMyRole(Principal principal, @RequestBody UserDTO body) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (body.getRole() == null) {
            return ApiResponseWrapper.error("role is required");
        }

        me.setRole(body.getRole());
        userRepo.save(me);

        return ApiResponseWrapper.ok(
                new UserDTO(
                        me.getId(),
                        me.getEmail(),
                        me.getUsername(),
                        null,
                        me.getStatus(),
                        me.getEmailVerifiedAt(),
                        me.getEnabled(),
                        me.getCreatedAt(),
                        me.getRole(),
                        me.getInstitute(),
                        me.getFaculty(),
                        me.getSubjects()
                )
        );
    }

    @PutMapping("/card")
    public ApiResponseWrapper<UserDTO> updateMyCard(Principal principal, @RequestBody UserDTO body) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        me.setInstitute(body.getInstitute());
        me.setFaculty(body.getFaculty());

        if (body.getSubjects() != null) {
            me.setSubjects(body.getSubjects());
        }

        userRepo.save(me);

        return ApiResponseWrapper.ok(
                new UserDTO(
                        me.getId(),
                        me.getEmail(),
                        me.getUsername(),
                        null,
                        me.getStatus(),
                        me.getEmailVerifiedAt(),
                        me.getEnabled(),
                        me.getCreatedAt(),
                        me.getRole(),
                        me.getInstitute(),
                        me.getFaculty(),
                        me.getSubjects()
                )
        );
    }
}