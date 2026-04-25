package DTO;

import java.time.Instant;

public class BlogDTO {

    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;
    private Instant createdAt;
    private Instant updatedAt;
    private String clientId;

    private Long likesCount;
    private Long commentsCount;
    private Boolean likedByMe;

    public BlogDTO() {}

    public BlogDTO(
            Long id,
            String title,
            String content,
            Long authorId,
            String authorUsername,
            Instant createdAt,
            Instant updatedAt,
            String clientId,
            Long likesCount,
            Long commentsCount,
            Boolean likedByMe
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.clientId = clientId;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.likedByMe = likedByMe;
    }

    public Boolean getLikedByMe() {
        return likedByMe;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getClientId() { return clientId; }
    public long getLikesCount() { return likesCount; }
    public long getCommentsCount() { return commentsCount; }

    public void setLikedByMe(Boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }
}