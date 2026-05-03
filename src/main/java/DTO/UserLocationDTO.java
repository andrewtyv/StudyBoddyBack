package DTO;

import model.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public class UserLocationDTO {

    private Long userId;
    private String username;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
    private Double distanceKm;
    private UserRole userRole;
    private String school;
    private String faculty;
    private List<String> subjects;

    public UserLocationDTO() {
    }

    public UserLocationDTO(Long userId, String username, Double latitude, Double longitude, LocalDateTime updatedAt, UserRole userRole) {
        this.userId = userId;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
        this.distanceKm = distanceKm;
        this.userRole = userRole;
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

    public UserRole getUserRole() {return userRole; }

    public String getSchool() { return school; }

    public String getFaculty() { return faculty; }

    public List<String> getSubjects() { return subjects; }

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

    public void setSchool(String school) { this.school = school; }

    public void setFaculty(String faculty) { this.faculty = faculty; }

    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}