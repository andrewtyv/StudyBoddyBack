package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 31)
    private String status = "PENDING_VERIFICATION";

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    @Column
    String photoUrl;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(name = "dark_mode")
    private Boolean darkMode;

    @Column(name = "high_contrast")
    private Boolean highContrast;

    @Column(name = "shareLocation")
    private Boolean shareLocation;

    @Column
    private Boolean studyReminderEnabled = false;

    @Column
    private Integer studyReminderHour;

    @Column
    private Integer studyReminderMinute;


    // ===== Constructors =====



    public User() {
    }

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.status = "PENDING_VERIFICATION";
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id,
                String email,
                String username,
                String password,
                String status,
                LocalDateTime emailVerifiedAt,
                Boolean enabled,
                LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public Boolean getDarkMode() {
        return darkMode;
    }

    public Boolean getHighContrast() {
        return highContrast;
    }

    public Boolean getShareLocation() {
        return shareLocation;
    }

    public void setHighContrast(Boolean highContrast) {
        this.highContrast = highContrast;
    }

    public void setShareLocation(Boolean shareLocation) {
        this.shareLocation = shareLocation;
    }

    public void setDarkMode(Boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) {
        this.locationUpdatedAt = locationUpdatedAt;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public LocalDateTime getLocationUpdatedAt() {
        return locationUpdatedAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserRole getRole() { return role; }




    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRole(UserRole role) { this.role = role; }

    public Boolean getStudyReminderEnabled() {
        return studyReminderEnabled;
    }

    public Integer getStudyReminderHour() {
        return studyReminderHour;
    }

    public Integer getStudyReminderMinute() {
        return studyReminderMinute;
    }

    public void setStudyReminderEnabled(Boolean studyReminderEnabled) {
        this.studyReminderEnabled = studyReminderEnabled;
    }

    public void setStudyReminderHour(Integer studyReminderHour) {
        this.studyReminderHour = studyReminderHour;
    }

    public void setStudyReminderMinute(Integer studyReminderMinute) {
        this.studyReminderMinute = studyReminderMinute;
    }
}