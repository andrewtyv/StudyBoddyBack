package DTO;

import java.time.Instant;

public class BlogDTO {

    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;
    private Instant createdAt;

    public BlogDTO() {
    }

    public BlogDTO(Long id, String title, String content, Long authorId, String authorUsername, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public Long getAuthorId() {
        return this.authorId;
    }

    public String getAuthorUsername() {
        return this.authorUsername;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}