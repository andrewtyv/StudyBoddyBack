package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "institute", length = 255)
    private String institute;

    @Column(name = "faculty", length = 255)
    private String faculty;

    @ElementCollection(targetClass = Subject.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_subjects", joinColumns = @JoinColumn(name = "user_id"))

    @Column(name = "subject", nullable = false)
    @Enumerated(EnumType.STRING)
    private java.util.Set<Subject> subjects = new java.util.HashSet<>();

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

    // ===== Getters =====

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

    public String getInstitute() {
        return institute;
    }

    public String getFaculty() {
        return faculty;
    }


    // ===== Setters =====

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

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public java.util.Set<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(java.util.Set<Subject> subjects) {
        this.subjects = subjects;
    }
}