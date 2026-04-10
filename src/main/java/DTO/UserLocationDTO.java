package DTO;

import java.time.LocalDateTime;

public class UserLocationDTO {

    private Long userId;
    private String username;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
    private Double distanceKm;

    public UserLocationDTO() {
    }

    public UserLocationDTO(Long userId, String username, Double latitude, Double longitude, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
        this.distanceKm = distanceKm;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}