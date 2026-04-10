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

@RestController
@RequestMapping("/location")
public class LocationController {
    private final UserRepo userRepo;

    public LocationController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

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

    @GetMapping("/nearby")
    public ApiResponseWrapper<List<UserLocationDTO>> getNearbyUsers(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ApiResponseWrapper.error("Unauthorized");
        }

        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("User not found");
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

