package model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "blog_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blog_id", "user_id"})
)
public class BlogLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public BlogLike(){}
    public BlogLike (Blog blog, User user){
        this.blog = blog;
        this.user= user;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Blog getBlog() {
        return blog;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

}