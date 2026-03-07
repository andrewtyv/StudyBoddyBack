package controllers;

import DTO.*;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import repos.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/room")
public class RoomController {
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



    @PostMapping("/create-direct")
    public ApiResponseWrapper<RoomDTO> createRoom (Principal principal, @RequestBody Map<String,String> body){

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
            return ApiResponseWrapper.ok(new RoomDTO(roomRepo.findByDirectKey(directKey).getId(),"the room already created"));
        }

        Room room = new Room(RoomType.DIRECT,directKey);


        roomRepo.save(room);

        RoomMember roomMemberMe= new RoomMember(room, me,RoomMemberRole.OWNER);
        RoomMember roomMemberFriend = new RoomMember(room, friend,RoomMemberRole.OWNER);


        roomMemberRepo.save(roomMemberMe);
        roomMemberRepo.save(roomMemberFriend);

        return ApiResponseWrapper.ok(new RoomDTO(room.getId(), room.getRoomType(),room.getDirectKey(), "", Integer.valueOf(0)));

    }

    @GetMapping("/direct-rooms")
    public ApiResponseWrapper<List<RoomDTO>> getAllRooms(Principal principal) {

        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        List<RoomMember> memberships = roomMemberRepo.findByUser(me);
        List<RoomDTO> dto = new ArrayList<>();

        for (RoomMember membership : memberships) {
            Room room = membership.getRoom();

            long unread = messageRecipientRepo
                    .countByRecipient_IdAndReadFalseAndMessage_Room_Id(me.getId(), room.getId());

            if (room.getRoomType().equals(RoomType.DIRECT)) {
                dto.add(new RoomDTO(
                        room.getId(),
                        room.getRoomType(),
                        room.getDirectKey(),
                        "",
                        (int) unread
                ));
            }
        }

        return ApiResponseWrapper.ok(dto);
    }

    @PostMapping("/read")
    public ApiResponseWrapper<String> ReadMessages(Principal principal, @RequestBody Map<String,String> api){
        Long roomId = Long.parseLong(api.get("id"));
        Optional<Room> room = roomRepo.findById(roomId);
        User me = userRepo.findByUsername(principal.getName());

        boolean isMember = roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, me.getId());
        if (!isMember) {
            return ApiResponseWrapper.error("you are not a member of this room");
        }
        if (!room.isPresent()){
            return ApiResponseWrapper.error("room with provided id not found");
        }

        List<MessageRecipient> messageRecipient = messageRecipientRepo.findByRecipientAndRead(me, false);
        for (MessageRecipient recipient : messageRecipient) {
            if (recipient.getMessage().getRoom().equals(room.get())) {
                recipient.markRead();
                messageRecipientRepo.save(recipient);
            }
        }
        return ApiResponseWrapper.ok("read");
    }

    @GetMapping("/enter")
    public ApiResponseWrapper<List<MessageDTO>> enterRoom(Principal principal, @RequestBody Map<String,String> api){

        User me = userRepo.findByUsername(principal.getName());
        Long roomId = Long.parseLong(api.get("id"));
        Optional<Room> room = roomRepo.findById(roomId);

        if (!room.isPresent()) {
            return ApiResponseWrapper.error("room with provided id not found");

        }
        boolean isMember = roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, me.getId());
        if (!isMember) {
            return ApiResponseWrapper.error("you are not a member of this room");
        }
        List<MessageDTO> dto = new ArrayList<>();
        List<Message> messages = messageRepo.findByRoom_IdOrderByCreatedAtAsc(room.get().getId());

        for (Message message : messages) {
            dto.add(new MessageDTO(message.getContent(), message.getMessageType(), message.getSender().getUsername()));
        }

        return ApiResponseWrapper.ok(dto);


    }

}
