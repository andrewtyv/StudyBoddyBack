package model;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name = "blog_comments")
public class BlogComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // for offline sync
    @Column(unique = true)
    private String clientId;

    public BlogComment(Blog blog, User author , String content){
        this.blog = blog;
        this.author = author;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Blog getBlog() {
        return blog;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getClientId() {
        return clientId;
    }

    public User getAuthor() {
        return author;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}