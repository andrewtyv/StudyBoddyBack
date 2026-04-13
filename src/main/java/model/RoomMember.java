package model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMemberRole role;

    @Column(nullable = false)
    private java.time.Instant joinedAt = java.time.Instant.now();

    public RoomMember(){

    }

    public RoomMember (Room room, User user, RoomMemberRole roomMemberRole){
        this.room = room;
        this.user = user;
        this.role =roomMemberRole;
    }


    public Room getRoom(){
        return this.room;
    }

    public User getUser() {
        return this.user;
    }
    public RoomMemberRole getRole() { return this.role;}

    public void setRole(RoomMemberRole role) {
        this.role = role;
    }
}