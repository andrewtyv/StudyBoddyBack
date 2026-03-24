package DTO;

import model.RoomMemberRole;

public class MemberDTO {
    private String username;
    private RoomMemberRole role;

    public String getUsername() {
        return username;
    }

    public RoomMemberRole getRole() {
        return role;
    }
    public MemberDTO(String username, RoomMemberRole role){
        this.username = username;
        this.role = role;
    }
}
