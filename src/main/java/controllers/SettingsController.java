package controllers;

import DTO.ApiResponseWrapper;
import DTO.UserDTO;
import model.User;
import model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import repos.UserRepo;

import java.security.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/user")
@Tag(name = "User settings", description = "Endpoints for retrieving and updating current user profile settings")
public class SettingsController {

    @Autowired
    UserRepo userRepo;
    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user's profile information."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": {
                                                "id": 5,
                                                "email": "nazar@example.com",
                                                "username": "nazar",
                                                "password": null,
                                                "status": "ONLINE",
                                                "emailVerifiedAt": "2026-03-31T10:00:00",
                                                "enabled": true,
                                                "createdAt": "2026-03-20T15:30:00",
                                                "role": "STUDENT",
                                                "institute": "FIIT STU",
                                                "faculty": "Informatics",
                                                "subjects": "DBS, OOP, Networks"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "user not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Update my role",
            description = "Updates the authenticated user's role."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User role updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": {
                                                "id": 5,
                                                "email": "nazar@example.com",
                                                "username": "nazar",
                                                "password": null,
                                                "status": "ONLINE",
                                                "emailVerifiedAt": "2026-03-31T10:00:00",
                                                "enabled": true,
                                                "createdAt": "2026-03-20T15:30:00",
                                                "role": "TEACHER",
                                                "institute": "FIIT STU",
                                                "faculty": "Informatics",
                                                "subjects": "DBS, OOP, Networks"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "user not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "role is required",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Update my user card",
            description = "Updates the authenticated user's institute, faculty, and subjects."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User card updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": {
                                                "id": 5,
                                                "email": "nazar@example.com",
                                                "username": "nazar",
                                                "password": null,
                                                "status": "ONLINE",
                                                "emailVerifiedAt": "2026-03-31T10:00:00",
                                                "enabled": true,
                                                "createdAt": "2026-03-20T15:30:00",
                                                "role": "STUDENT",
                                                "institute": "STU",
                                                "faculty": "FIIT",
                                                "subjects": "Java, Spring, Databases"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "user not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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