package model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "room")
public class Room {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType roomType;


    // direct key = min(user1.id , user2.id) + ":" + max (user1.id + user2.id)
    @Column(name = "direct_key", unique = true)
    private String directKey;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoomMember> members = new java.util.HashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Room (){

    }
    public Room (RoomType roomType, String directKey){
        this.roomType= roomType;
        this.directKey=directKey;
    }

    public void setRoomType(RoomType roomType){
        this.roomType = roomType;
    }

    public Long getId(){
        return  this.id;
    }
    public RoomType getRoomType(){
        return this.roomType;
    }
    public String getDirectKey(){
        return this.directKey;
    }

    public Set<RoomMember> getMembers() {
        return this.members;
    }
}
