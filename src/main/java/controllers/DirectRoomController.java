package controllers;

import DTO.ApiResponseWrapper;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import repos.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/directRoom")
public class DirectRoomController {
    @Autowired
    MessageRecipientRepo messageRecipientRepo;

    @Autowired
    MessageRepo messageRepo;

    @Autowired
    RoomMemberRepo roomMemberRepo;

    @Autowired
    RoomRepo roomRepo;

    @Autowired
    UserRepo userRepo;


    @PostMapping("/create-room")
    public ApiResponseWrapper<String> createRoom (Principal principal, @RequestBody Map<String,String> body){
        String username = body.get("username");
        User friend = userRepo.findByUsername(username);
        User me = userRepo.findByUsername(principal.getName());
        if (friend == null || me == null) {
            return ApiResponseWrapper.error("username is not found");
        }

        if (friend.getId().equals(me.getId())) {
            return ApiResponseWrapper.error("cannot create direct room with yourself");
        }

        String directKey = ((Long)Math.min(friend.getId(), me.getId())).toString()
                + ":" +
                ((Long)Math.max(friend.getId(), me.getId())).toString();


        if (roomRepo.findByDirectKey(directKey)!= null) {
            return ApiResponseWrapper.ok("room between this users already exist");
        }

        Room room = new Room(RoomType.DIRECT,directKey);


        roomRepo.save(room);

        RoomMember roomMemberMe= new RoomMember(room, me,RoomMemberRole.OWNER);
        RoomMember roomMemberFriend = new RoomMember(room, friend,RoomMemberRole.OWNER);


        roomMemberRepo.save(roomMemberMe);
        roomMemberRepo.save(roomMemberFriend);

        return ApiResponseWrapper.ok("room succesfully created, enjoy ur chat");

    }
}
