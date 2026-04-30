package DTO;

import model.Subject;
import java.time.Instant;

public class BlogDTO {

    private Long id;
    private String title;
    private String content;
    private Subject subject;

    private Long authorId;
    private String authorUsername;

    private Instant createdAt;
    private Instant updatedAt;

    private Long likesCount;
    private Long commentsCount;
    private Boolean likedByMe;

    public BlogDTO() {}

    public BlogDTO(
            Long id,
            String title,
            String content,
            Subject subject,
            Long authorId,
            String authorUsername,
            Instant createdAt,
            Instant updatedAt,
            Long likesCount,
            Long commentsCount,
            Boolean likedByMe
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.likedByMe = likedByMe;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Subject getSubject() { return subject; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getLikesCount() { return likesCount; }
    public Long getCommentsCount() { return commentsCount; }
    public Boolean getLikedByMe() { return likedByMe; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setLikesCount(Long likesCount) { this.likesCount = likesCount; }
    public void setCommentsCount(Long commentsCount) { this.commentsCount = commentsCount; }
    public void setLikedByMe(Boolean likedByMe) { this.likedByMe = likedByMe; }
}