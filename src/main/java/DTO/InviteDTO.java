package DTO;

public class InviteDTO {

    private Long id;
    private Long roomId;
    private String roomType;
    private String roomName;
    private String inviterUsername;

    public InviteDTO() {
    }

    public InviteDTO(Long id, Long roomId, String roomName, String inviterUsername) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.inviterUsername = inviterUsername;
    }

    // ===== GETTERS =====

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    // ===== SETTERS =====

    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }
}