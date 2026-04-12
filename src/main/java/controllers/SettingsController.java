package controllers;

import DTO.ApiResponseWrapper;
import DTO.SettingsDTO;
import DTO.StudentProfileDTO;
import DTO.UserDTO;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import repos.FriendshipRepo;
import repos.StudentProfileRepo;
import repos.UserBlockRepo;
import repos.UserRepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
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

    @Autowired
    UserBlockRepo userBlockRepo;

    @Autowired
    FriendshipRepo friendshipRepo;

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

    @Operation(
            summary = "Get my student card",
            description = "Returns the authenticated user's student profile card."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Student profile returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": {
                                            "school": "STU",
                                            "faculty": "FIIT",
                                            "subjects": ["JAVA", "DATABASES", "NETWORKS"]
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User or profile not found",
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
                                          "message": "profile not found",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Upload avatar",
            description = "Uploads a new avatar image for the authenticated user. Allowed formats: jpg, jpeg, png, webp."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Avatar uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": "/uploads/user-avatar/user_5/abc123.jpg"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or upload error",
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
                                          "message": "file is empty",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "only jpg, jpeg, png, webp are allowed",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "upload failed",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Get my avatar",
            description = "Returns the avatar URL of the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Avatar URL returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": "/uploads/user-avatar/user_5/abc123.jpg"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Authenticated user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": false,
                                          "message": "user doesnt exist",
                                          "data": null
                                        }
                                        """
                            )
                    )
            )
    })
    @GetMapping("/avatar")
    public ApiResponseWrapper<String> getAvatar(Principal principal){
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user doesnt exist");
        }
        return ApiResponseWrapper.ok(me.getPhotoUrl());
    }
    @Operation(
            summary = "Update user settings",
            description = "Updates the authenticated user's accessibility, privacy, reminder, and push notification settings."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Settings updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": "data set"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or lookup error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "this user doesn't exist",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "all nulls",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @PutMapping("/settings")
    public ApiResponseWrapper<String> setSettings(Principal principal,@RequestBody SettingsDTO dto){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("this user doesn't exist");
        }

        if (dto.getDarkMode() == null && dto.getHighContrast() == null && dto.getShareLocation() ==null && dto.getPushNotifications() == null) {
            return ApiResponseWrapper.error("all nulls");
        }


        if (dto.getDarkMode() != null)
            me.setDarkMode(dto.getDarkMode());

        if (dto.getHighContrast() != null)
            me.setHighContrast(dto.getHighContrast());

        if (dto.getShareLocation() != null)
            me.setShareLocation(dto.getShareLocation());
        if(dto.getStudyReminderEnabled() == Boolean.TRUE && dto.getStudyReminderHour() !=null && dto.getStudyReminderMinute()!= null)
        {
            me.setStudyReminderMinute(dto.getStudyReminderMinute());
            me.setStudyReminderHour(dto.getStudyReminderHour());
            me.setStudyReminderEnabled(dto.getStudyReminderEnabled());
        }

        if (dto.getPushNotifications() != null) {
            me.setPushNotificationsEnabled(dto.getPushNotifications());
        }

        userRepo.save(me);

        return ApiResponseWrapper.ok("data set");
    }
    @Operation(
            summary = "Get user settings",
            description = "Returns the authenticated user's current settings."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Settings returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": {
                                            "darkMode": true,
                                            "highContrast": false,
                                            "shareLocation": true,
                                            "studyReminderEnabled": true,
                                            "studyReminderHour": 18,
                                            "studyReminderMinute": 30,
                                            "pushNotifications": true
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Authenticated user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": false,
                                          "message": "this user doesn't exist",
                                          "data": null
                                        }
                                        """
                            )
                    )
            )
    })
    @GetMapping("/settings")
    public ApiResponseWrapper<SettingsDTO> getSettings(Principal principal){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("this user doesn't exist");
        }
        return ApiResponseWrapper.ok(
                new SettingsDTO(me.getDarkMode(),me.getHighContrast(),me.getShareLocation(), me.getStudyReminderEnabled(),me.getStudyReminderHour(),me.getStudyReminderMinute(), me.getPushNotificationsEnabled())
        );
    }

    @Operation(
            summary = "Save Expo push token",
            description = "Saves or updates the Expo push token for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Push token saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": "push token saved"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or lookup error",
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
                                          "message": "expoPushToken is required",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @PutMapping("/push-token")
    public ApiResponseWrapper<String> savePushToken(Principal principal, @RequestBody java.util.Map<String, String> body
    ) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (body == null || body.get("expoPushToken") == null || body.get("expoPushToken").isBlank()) {
            return ApiResponseWrapper.error("expoPushToken is required");
        }

        me.setExpoPushToken(body.get("expoPushToken").trim());
        userRepo.save(me);
        return ApiResponseWrapper.ok("push token saved");
    }
    @Operation(
            summary = "Clear Expo push token",
            description = "Deletes the saved Expo push token for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Push token cleared successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "ok",
                                          "data": "push token cleared"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
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
    @DeleteMapping("/push-token")
    public ApiResponseWrapper<String> clearPushToken(Principal principal) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        me.setExpoPushToken(null);
        userRepo.save(me);

        return ApiResponseWrapper.ok("push token cleared");
    }



    @PostMapping("/block-user")
    public ApiResponseWrapper<String> blockUser(
            Principal principal,
            @RequestBody Map<String, String> api
    ) {
        User me = userRepo.findByUsername(principal.getName());
        User enemy = userRepo.findByUsername(api.get("username"));

        if (me == null || enemy == null) {
            return ApiResponseWrapper.error("user doesn't exist");
        }

        if (me.getId().equals(enemy.getId())) {
            return ApiResponseWrapper.error("you cannot block yourself");
        }

        if (userBlockRepo.existsByBlockedAndBlocker(enemy, me)) {
            return ApiResponseWrapper.error("you already blocked this user");
        }

        if (userBlockRepo.existsByBlockedAndBlocker(me, enemy)) {
            return ApiResponseWrapper.error("this user has blocked you");
        }

        UserBlock block = new UserBlock(me,enemy);
        userBlockRepo.save(block);

        Friendship friendship =
                friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                        me.getId(), enemy.getId(),
                        enemy.getId(), me.getId()
                );

        if (friendship != null) {
            friendshipRepo.delete(friendship);
        }

        return ApiResponseWrapper.ok("blocked successfully");
    }

    @GetMapping("/block-user")
    public ApiResponseWrapper<List<UserDTO>> getBlocked(Principal principal){

        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("u dont exist");
        }
        List<UserBlock> blocked = userBlockRepo.findByBlocker(me);
        return ApiResponseWrapper.ok(blocked.stream().map(
                    fr-> new UserDTO(fr.getBlocked().getUsername())
                ).toList());
    }

    @DeleteMapping("/block-user")
    public ApiResponseWrapper<String> unblockUser(Principal principal, @RequestBody Map<String,String> api) {
        User me = userRepo.findByUsername(principal.getName());
        User nowFriend = userRepo.findByUsername(api.get("username"));

        if (me == null || nowFriend ==null) {
            return ApiResponseWrapper.error("dont exist");
        }

        UserBlock block = userBlockRepo.findByBlockedAndAndBlocker(nowFriend,me);

        if (block ==null) {
            return ApiResponseWrapper.error("u didnt block this user");
        }

        userBlockRepo.delete(block);
        return ApiResponseWrapper.ok("unblocked succesfully");

    }

}




