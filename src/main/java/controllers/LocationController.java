package controllers;

import DTO.ApiResponseWrapper;
import DTO.UserLocationDTO;
import DTO.LocationUpdateRequestDTO;
import model.User;
import org.springframework.web.bind.annotation.*;
import repos.UserRepo;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/location")
@Tag(name = "Location", description = "Endpoints for updating and retrieving user geolocation")
public class    LocationController {
    private final UserRepo userRepo;

    public LocationController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }
    @Operation(
            summary = "Update my location",
            description = "Updates the authenticated user's current latitude and longitude."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Location updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "Location updated successfully",
                                          "data": null,
                                          "token": null
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
                                          "message": "Latitude and longitude are required",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "Latitude must be between -90 and 90",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "Longitude must be between -180 and 180",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "User not found",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @PutMapping("/updateLocation")
    public ApiResponseWrapper<String> updateMyLocation(
            Principal principal,
            @RequestBody LocationUpdateRequestDTO request
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ApiResponseWrapper.error("Unauthorized");
        }

        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return ApiResponseWrapper.error("Latitude and longitude are required");
        }

        double latitude = request.getLatitude();
        double longitude = request.getLongitude();

        if (latitude < -90 || latitude > 90) {
            return ApiResponseWrapper.error("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            return ApiResponseWrapper.error("Longitude must be between -180 and 180");
        }

        User user = userRepo.findByUsername(principal.getName());
        if (user == null) {
            return ApiResponseWrapper.error("User not found");
        }

        if(user.getShareLocation() != true) {
            return ApiResponseWrapper.error("Share location disabled");
        }

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setLocationUpdatedAt(LocalDateTime.now());
        userRepo.save(user);

        return new ApiResponseWrapper<>(
                true,
                "Location updated successfully",
                null,
                null
        );
    }
    @Operation(
            summary = "Get my location",
            description = "Returns the authenticated user's saved location."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Location fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "Location fetched successfully",
                                          "data": {
                                            "userId": 5,
                                            "username": "nazar",
                                            "latitude": 48.1486,
                                            "longitude": 17.1077,
                                            "updatedAt": "2026-04-10T21:30:00"
                                          },
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Location not available",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "User not found",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "Location not set",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @GetMapping("/me")
    public ApiResponseWrapper<UserLocationDTO> getMyLocation(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ApiResponseWrapper.error("Unauthorized");
        }

        User user = userRepo.findByUsername(principal.getName());
        if (user == null) {
            return ApiResponseWrapper.error("User not found");
        }

        if (user.getLatitude() == null || user.getLongitude() == null) {
            return ApiResponseWrapper.error("Location not set");
        }

        if(user.getShareLocation() != true) {
            return ApiResponseWrapper.error("Share location disabled");
        }

        return new ApiResponseWrapper<>(
                true,
                "Location fetched successfully",
                toDto(user),
                null
        );
    }
    @Operation(
            summary = "Get nearby users",
            description = "Returns all users with saved location ordered from nearest to farthest relative to the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Nearby users fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "Nearby users fetched successfully",
                                          "data": [
                                            {
                                              "userId": 7,
                                              "username": "john",
                                              "latitude": 48.1501,
                                              "longitude": 17.1102,
                                              "updatedAt": "2026-04-10T21:31:00",
                                              "distanceKm": 0.284
                                            },
                                            {
                                              "userId": 12,
                                              "username": "anna",
                                              "latitude": 48.1700,
                                              "longitude": 17.1500,
                                              "updatedAt": "2026-04-10T21:32:00",
                                              "distanceKm": 3.912
                                            }
                                          ],
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Location unavailable for current user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "User not found",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "Your location is not set",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @GetMapping("/nearby")
    public ApiResponseWrapper<List<UserLocationDTO>> getNearbyUsers(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ApiResponseWrapper.error("Unauthorized");
        }

        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("User not found");
        }

        if(me.getShareLocation() != true) {
            return ApiResponseWrapper.error("Share location disabled");
        }

        if (me.getLatitude() == null || me.getLongitude() == null) {
            return ApiResponseWrapper.error("Your location is not set");
        }

        double myLat = me.getLatitude();
        double myLon = me.getLongitude();

        List<UserLocationDTO> result = userRepo.findAll().stream()
                .filter(user -> !user.getId().equals(me.getId()))
                .filter(user -> user.getLatitude() != null && user.getLongitude() != null)
                .map(user -> {
                    UserLocationDTO dto = toDto(user);
                    dto.setDistanceKm(roundTo3(haversineKm(
                            myLat,
                            myLon,
                            user.getLatitude(),
                            user.getLongitude()
                    )));
                    return dto;
                })
                .sorted(Comparator.comparing(UserLocationDTO::getDistanceKm))
                .toList();

        return new ApiResponseWrapper<>(
                true,
                "Nearby users fetched successfully",
                result,
                null
        );
    }
    @Operation(
            summary = "Get location of a specific user",
            description = "Returns the saved location of the specified user by username."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Location fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": "Location fetched successfully",
                                          "data": {
                                            "userId": 7,
                                            "username": "john",
                                            "latitude": 48.1501,
                                            "longitude": 17.1102,
                                            "updatedAt": "2026-04-10T21:31:00"
                                          },
                                          "token": null
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
                                          "message": "Username is required",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "User not found",
                                          "data": null
                                        }
                                        """),
                                    @ExampleObject(value = """
                                        {
                                          "success": false,
                                          "message": "Location not set",
                                          "data": null
                                        }
                                        """)
                            }
                    )
            )
    })
    @GetMapping("/{username}")
    public ApiResponseWrapper<UserLocationDTO> getUserLocation(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ApiResponseWrapper.error("Username is required");
        }

        User user = userRepo.findByUsername(username.trim());
        if (user == null) {
            return ApiResponseWrapper.error("User not found");
        }

        if (user.getLatitude() == null || user.getLongitude() == null) {
            return ApiResponseWrapper.error("Location not set");
        }

        if (!Boolean.TRUE.equals(user.getShareLocation())) {
            return ApiResponseWrapper.error("Share location disabled");
        }
        return new ApiResponseWrapper<>(
                true,
                "Location fetched successfully",
                toDto(user),
                null
        );
    }

    private UserLocationDTO toDto(User user) {
        return new UserLocationDTO(
                user.getId(),
                user.getUsername(),
                user.getLatitude(),
                user.getLongitude(),
                user.getLocationUpdatedAt(),
                user.getRole()
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double roundTo3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}

