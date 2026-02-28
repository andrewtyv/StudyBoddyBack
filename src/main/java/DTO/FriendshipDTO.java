package DTO;


import java.time.LocalDateTime;

public class FriendshipDTO {

    private Long id;
    private String username;
    private String status;
    private LocalDateTime createdAt;

    public FriendshipDTO() {
    }

    public FriendshipDTO(Long id,
                         String username,
                         String status,
                         LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
