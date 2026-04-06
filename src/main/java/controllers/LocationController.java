package controllers;

import DTO.ApiResponseWrapper;
import DTO.UserLocationDTO;
import DTO.LocationUpdateRequestDTO;
import model.User;
import org.springframework.web.bind.annotation.*;
import repos.UserRepo;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/user/location")
public class LocationController {
    private final UserRepo userRepo;

    public LocationController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @PutMapping
    public ApiResponseWrapper<UserLocationDTO> updateMyLocation(
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

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setLocationUpdatedAt(LocalDateTime.now());
        userRepo.save(user);

        return new ApiResponseWrapper<>(
                true,
                "Location updated successfully",
                toDto(user),
                null
        );
    }

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

        return new ApiResponseWrapper<>(
                true,
                "Location fetched successfully",
                toDto(user),
                null
        );
    }

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
                user.getLocationUpdatedAt()
        );
    }
}

