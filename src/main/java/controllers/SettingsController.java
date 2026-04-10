package controllers;

import DTO.ApiResponseWrapper;
import DTO.SettingsDTO;
import DTO.StudentProfileDTO;
import DTO.UserDTO;
import model.StudentProfile;
import model.User;
import model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import repos.StudentProfileRepo;
import repos.UserRepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;

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

    @Autowired
    StudentProfileRepo studentProfileRepo;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

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
                        me.getRole()
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
                        me.getRole()
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
    @PutMapping("/update-card")
    public ApiResponseWrapper<String> updateMyCard(Principal principal, @RequestBody StudentProfileDTO body) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (me.getRole() != UserRole.STUDENT) {
            return ApiResponseWrapper.error("only students can have a student profile");
        }

        if (body.getSchool() == null || body.getSchool().isBlank()) {
            return ApiResponseWrapper.error("school is required");
        }

        if (body.getFaculty() == null || body.getFaculty().isBlank()) {
            return ApiResponseWrapper.error("faculty is required");
        }

        if (body.getSubjects() == null || body.getSubjects().isEmpty()) {
            return ApiResponseWrapper.error("at least one subject is required");
        }

        StudentProfile profile = studentProfileRepo.findByUser(me);

        if (profile == null) {
            profile = new StudentProfile();
            profile.setUser(me);
        }

        profile.setSchool(body.getSchool());
        profile.setFaculty(body.getFaculty());
        profile.setSubjects(body.getSubjects());

        studentProfileRepo.save(profile);

        return ApiResponseWrapper.ok("updated successfully");
    }

    @GetMapping("/card")
    public ApiResponseWrapper<StudentProfileDTO> getMyCard(Principal principal) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        StudentProfile profile = studentProfileRepo.findByUser(me);

        if (profile== null) {
            return ApiResponseWrapper.error("profile not found");
        }

        return ApiResponseWrapper.ok(new StudentProfileDTO(profile));
    }

    @PostMapping("/upload-avatar")
    public ApiResponseWrapper<String> uploadAvatar(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (file == null || file.isEmpty()) {
            return ApiResponseWrapper.error("file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ApiResponseWrapper.error("only jpg, jpeg, png, webp are allowed");
        }

        try {
            Path avatarUploadPath = Paths.get(uploadDir, "user-avatar", "user_" + me.getId());
            Files.createDirectories(avatarUploadPath);

            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "avatar.jpg" : file.getOriginalFilename()
            );

            String extension = getExtension(originalName, contentType);
            String storedName = UUID.randomUUID() + extension;

            Path targetPath = avatarUploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = "/uploads/user-avatar/user_" + me.getId() + "/" + storedName;

            me.setPhotoUrl(relativeUrl);
            userRepo.save(me);

            return ApiResponseWrapper.ok(relativeUrl);

        } catch (IOException e) {
            return ApiResponseWrapper.error("upload failed");
        }
    }
    private String getExtension(String originalName, String contentType) {
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            return originalName.substring(dotIndex);
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    @PutMapping("/settings")
    public ApiResponseWrapper<String> setSettings(Principal principal,@RequestBody SettingsDTO dto){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("this user doesn't exist");
        }

        if (dto.getDarkMode() == null && dto.getHighContrast() == null && dto.getShareLocation() ==null) {
            return ApiResponseWrapper.error("all nulls");
        }


        if (dto.getDarkMode() != null)
            me.setDarkMode(dto.getDarkMode());

        if (dto.getHighContrast() != null)
            me.setHighContrast(dto.getHighContrast());

        if (dto.getShareLocation() != null)
            me.setShareLocation(dto.getShareLocation());

        userRepo.save(me);

        return ApiResponseWrapper.ok("data set");
    }

    @GetMapping("/settings")
    public ApiResponseWrapper<SettingsDTO> getSettings(Principal principal){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("this user doesn't exist");
        }
        return ApiResponseWrapper.ok(
                new SettingsDTO(me.getDarkMode(),me.getHighContrast(),me.getShareLocation())
        );
    }






}




