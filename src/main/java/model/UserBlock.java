package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_block")
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column
    private LocalDateTime created_at;


    public UserBlock(User blocker, User blocked)
    {
        this.blocked = blocked;
        this.blocker = blocker;
        this.created_at = LocalDateTime.now();
    }

    public void setBlocked(User blocked) {
        this.blocked = blocked;
    }

    public void setBlocker(User blocker) {
        this.blocker = blocker;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public Long getId() {
        return id;
    }

    public User getBlocked() {
        return blocked;
    }

    public User getBlocker() {
        return blocker;
    }
}
