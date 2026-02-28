package model;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendship")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime friendshipSentAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime friendshipUpdatedAt;


    public FriendshipStatus getStatus(){
        return this.status;
    }

    public Friendship(){

    }

    public Friendship(User requester, User addressee){
        this.requester = requester;
        this.status = FriendshipStatus.PENDING;
        this.addressee = addressee;
        this.friendshipSentAt = LocalDateTime.now();
        this.friendshipUpdatedAt = LocalDateTime.now();


    }

    public Long getId(){
        return this.id;
    }
    public User getRequester(){
        return this.requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public LocalDateTime getFriendshipSentAt() {
        return friendshipSentAt;
    }

    public LocalDateTime getFriendshipUpdatedAt() {
        return friendshipUpdatedAt;
    }
    public void setStatus(FriendshipStatus status){
        this.status = status;
    }

}

