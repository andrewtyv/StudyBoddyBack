package DTO;

import model.RoomType;

public class RoomDTO {
    private Long id;

    private RoomType roomType;

    private String directKey;

    private String message;

    private Integer unread;

    public  RoomDTO(){}

    public RoomDTO(Long id, RoomType roomType, String directKey, String message, Integer unread){
        this.id = id;
        this.roomType = roomType;
        this.directKey = directKey;
        this.message = message;
        this.unread = unread;
    }
    public RoomDTO(Long id,String message){
        this.id=id;
        this.message = message;
    }
    public RoomDTO(Long id){
        this.id= id;
    }

    public String getMessage(){
        return this.message;
    }
    public String getDirectKey(){
        return  this.directKey;
    }
    public  Long getId (){
        return this.id;
    }
    public RoomType getRoomType(){
        return this.roomType;
    }


}
