package DTO;

import model.UserRole;
import java.time.LocalDateTime;
import model.Subject;
import java.util.Set;

public class UserDTO {

    private Long id;
    private String email;
    private String username;
    private String password;
    private String status;
    private LocalDateTime emailVerifiedAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private UserRole role;



    public UserDTO() {
    }

    public UserDTO(String username){
        this.username = username;
    }

    public UserDTO(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.status = "PENDING_VERIFICATION";
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public UserDTO(Long id,
                   String email,
                   String username,
                   String password,
                   String status,
                   LocalDateTime emailVerifiedAt,
                   Boolean enabled,
                   LocalDateTime createdAt,
                   UserRole role) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.role = role;

    }

    // ===== GETTERS =====

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



    // ===== SETTERS =====

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

    public void  setRole (UserRole role) { this.role = role; }

}


