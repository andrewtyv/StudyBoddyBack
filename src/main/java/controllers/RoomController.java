package controllers;

import DTO.*;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import repos.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Controller responsible for room and messaging operations.
 *
 * <p>This controller provides endpoints for creating direct rooms,
 * retrieving rooms of the authenticated user, marking messages as read,
 * loading messages from a room, and sending messages through WebSocket.</p>
 */
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

    @Autowired
    RoomInviteRepo roomInviteRepo;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    FriendshipRepo friendshipRepo;


    /**
     * Creates a direct room between the authenticated user and another user.
     *
     * <p>The target user is provided in the request body under {@code username}.
     * If both users exist and are different, a unique direct key is generated.
     * If a direct room between the same two users already exists, the existing
     * room is returned instead of creating a new one.</p>
     *
     * @param principal authenticated user
     * @param body map containing the target username under {@code username}
     * @return an {@link ApiResponseWrapper} containing information about the created
     *         or already existing direct room
     */
    @PostMapping("/create-direct")
    public ApiResponseWrapper<RoomDTO> createRoom (Principal principal, @RequestBody Map<String,String> body){
        System.out.println("here direct");
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
            System.out.println("\n"+directKey.toString() + "\n");
            return ApiResponseWrapper.ok(new RoomDTO(roomRepo.findByDirectKey(directKey).getId(),"the room already created"));
        }

        Room room = new Room(RoomType.DIRECT,directKey);


        roomRepo.save(room);

        RoomMember roomMemberMe= new RoomMember(room, me,RoomMemberRole.OWNER);
        RoomMember roomMemberFriend = new RoomMember(room, friend,RoomMemberRole.OWNER);


        roomMemberRepo.save(roomMemberMe);
        roomMemberRepo.save(roomMemberFriend);

        System.out.println("succesfully created");
        return ApiResponseWrapper.ok(new RoomDTO(room.getId(), room.getRoomType(),room.getDirectKey(), "", Integer.valueOf(0)));

    }
    @PostMapping("/group-room")
    public ApiResponseWrapper<RoomDTO> createGroupRoom(Principal principal, @RequestBody Map<String,String> body) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        String groupName = body.get("name");
        if (groupName == null || groupName.trim().isBlank()) {
            return ApiResponseWrapper.error("group name is required");
        }

        Room room = new Room(RoomType.GROUP, null);
        room.setRoomName(groupName.trim());
        roomRepo.save(room);

        RoomMember owner = new RoomMember(room, me, RoomMemberRole.OWNER);
        roomMemberRepo.save(owner);

        return ApiResponseWrapper.ok(new RoomDTO(room.getId(), RoomType.GROUP, null, groupName.trim(), 0));

    }

    @PostMapping("/create-invite")
    public ApiResponseWrapper<String> create_invite(Principal principal, @RequestBody Map<String,String> api){
        User me = userRepo.findByUsername(principal.getName());
        User friend = userRepo.findByUsername(api.get("username").trim());
        Room room = roomRepo.findById(Long.parseLong(api.get("id"))).get();
        if (friend == null || me ==null) {
            return ApiResponseWrapper.error("something is null");
        }
        if (me.getId().equals(friend.getId())) {
            return ApiResponseWrapper.error("You cannot invite yourself");
        }

        if (room.getRoomType() == RoomType.DIRECT) {
            return ApiResponseWrapper.error("Cannot invite users to direct room");
        }

        if (roomInviteRepo.existsByInviteeIdAndRoomIdAndStatus(friend.getId(), room.getId(), RoomInviteStatus.PENDING)) {
            return ApiResponseWrapper.error("Invite already exists");
        }
        if (!roomMemberRepo.existsByRoomIdAndUserId(room.getId(), me.getId())) {
            return ApiResponseWrapper.error("You are not a member of this room");
        }

        if (roomMemberRepo.existsByRoomIdAndUserId(room.getId(), friend.getId())) {
            return ApiResponseWrapper.error("User is already a member of this room");
        }
        RoomMember membership = roomMemberRepo.findByRoomIdAndUserId(room.getId(), me.getId());

        if (membership == null) {
            return ApiResponseWrapper.error("You are not a member of this room");
        }

        if (membership.getRole() != RoomMemberRole.OWNER &&
                membership.getRole() != RoomMemberRole.ADMIN) {
            return ApiResponseWrapper.error("You don't have permission to invite users");
        }


        RoomInvite invite = new RoomInvite(room, me, friend);
        roomInviteRepo.save(invite);
        return ApiResponseWrapper.ok("invite created succesfully");

    }
    @GetMapping("/my-invites")
    public ApiResponseWrapper<List<InviteDTO>> myInvites(Principal principal) {

        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("User not found");
        }

        List<RoomInvite> invites = roomInviteRepo
                .findAllByInviteeIdAndStatus(me.getId(), RoomInviteStatus.PENDING);

        List<InviteDTO> response = invites.stream()
                .map(invite -> new InviteDTO(
                        invite.getId(),
                        invite.getRoom().getId(),
                        invite.getRoom().getRoomName(),
                        invite.getInviter().getUsername()
                ))
                .toList();

        return ApiResponseWrapper.ok(response);
    }

    @PostMapping("/accept-invite")
    public ApiResponseWrapper<String> accept(Principal principal, @RequestBody  Map<String,String> api){
        User me = userRepo.findByUsername(principal.getName());
        Long inviteId = Long.parseLong(api.get("inviteId"));
        RoomInvite invite = roomInviteRepo.findById(inviteId).orElse(null);
        if (!invite.getInvitee().getId().equals(me.getId())) {
            return ApiResponseWrapper.error("This is not your invite");
        }

        if (invite.getStatus() != RoomInviteStatus.PENDING) {
            return ApiResponseWrapper.error("Invite already processed");
        }

        if (roomMemberRepo.existsByRoomIdAndUserId(invite.getRoom().getId(), me.getId())) {
            return ApiResponseWrapper.error("Already in room");
        }
        invite.accept();
        roomInviteRepo.save(invite);

        RoomMember member = new RoomMember(invite.getRoom(), me,RoomMemberRole.MEMBER);
        roomMemberRepo.save(member);

        return ApiResponseWrapper.ok("accepted");

    }

    @PostMapping("/decline-invite")
    public ApiResponseWrapper<String> decline(Principal principal, @RequestBody  Map<String,String> api){
        User me = userRepo.findByUsername(principal.getName());
        Long inviteId = Long.parseLong(api.get("inviteId"));
        RoomInvite invite = roomInviteRepo.findById(inviteId).orElse(null);
        if (!invite.getInvitee().getId().equals(me.getId())) {
            return ApiResponseWrapper.error("This is not your invite");
        }

        if (invite.getStatus() != RoomInviteStatus.PENDING) {
            return ApiResponseWrapper.error("Invite already processed");
        }

        if (roomMemberRepo.existsByRoomIdAndUserId(invite.getRoom().getId(), me.getId())) {
            return ApiResponseWrapper.error("Already in room");
        }
        invite.decline();
        roomInviteRepo.save(invite);

        return ApiResponseWrapper.ok("accepted");
    }

        /**
         * Returns all direct rooms of the authenticated user.
         *
         * <p>This endpoint retrieves all room memberships of the authenticated user,
         * calculates the number of unread messages in each room, and returns only
         * rooms of type {@code DIRECT}.</p>
         *
         * @param principal authenticated user
         * @return an {@link ApiResponseWrapper} containing the list of direct rooms
         */
    @GetMapping("/all-rooms")
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
            String title ="";
            if (room.getRoomType() == RoomType.DIRECT) {
                Set<RoomMember> members = room.getMembers();
                for (RoomMember member : members) {
                    if (!member.getUser().getUsername().equals(principal.getName())) {
                        title = member.getUser().getUsername();
                    }
                }
            }
            if (room.getRoomType() == RoomType.GROUP) {
                title = room.getRoomName();
            }

            dto.add(new RoomDTO(
                    room.getId(),
                    room.getRoomType(),
                    room.getDirectKey(),
                    title,
                    (int) unread
            ));
        }
        return ApiResponseWrapper.ok(dto);
    }




    /**
     * Marks all unread messages in the specified room as read for the authenticated user.
     *
     * <p>The room identifier is provided in the request body under {@code id}.
     * The operation is performed only if the room exists and the authenticated
     * user is a member of the room.</p>
     *
     * @param principal authenticated user
     * @param api map containing the room identifier under {@code id}
     * @return an {@link ApiResponseWrapper} containing the result of the operation
     */
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
    /**
     * Returns all messages from the specified room for the authenticated user.
     *
     * <p>The room identifier is provided in the request body under {@code id}.
     * The room must exist and the authenticated user must be a member of it.
     * Messages are returned in ascending order by creation time.</p>
     *
     * @param principal authenticated user
     * @param api map containing the room identifier under {@code id}
     * @return an {@link ApiResponseWrapper} containing the list of room messages
     */
    @PostMapping("/enter")
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

    /**
     * Sends a message to a room through WebSocket.
     *
     * <p>This handler reads the room identifier, message content, and message type
     * from the incoming payload. It verifies that the authenticated user exists,
     * that the message content is not empty, and that the user is a member of the room.
     * The message is then saved, unread recipient records are created for other members,
     * and the message is broadcast to subscribers of the room topic.</p>
     *
     * @param principal authenticated user
     * @param api map containing {@code roomId}, {@code content}, and {@code messageType}
     */
    /*
        roomId: ""
        content: ""
        messageType: ""
     */
    @MessageMapping("send-message")
    public void sendMessage(Principal principal, Map<String,String> api){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return;
        }
        String content = api.get("content");
        Long id = Long.parseLong(api.get("roomId"));
        Optional<Room> room = roomRepo.findById(id);
        MessageType messageType = MessageType.valueOf(api.get("messageType")) ;


        if(content.isBlank() || content.isEmpty()) {
            System.out.println("here1");
            return;
        }

        if (!roomMemberRepo.existsByRoom_IdAndUser_Id(id, me.getId())) {
            System.out.println("here2");
            return;
        }
        Message message = new Message(messageType, room.get(), me,content );
        messageRepo.save(message);

        List<RoomMember> members = roomMemberRepo.findByRoom_Id(room.get().getId());
        List<MessageRecipient> recipients = new ArrayList<>();

        for (RoomMember member : members) {
            if (!member.getUser().getId().equals(me.getId())) {
                recipients.add(new MessageRecipient(member.getUser() , message));
            }
        }

        messageRecipientRepo.saveAll(recipients);

        MessageWsDTO dto = new MessageWsDTO(
                message.getId(),
                room.get().getId(),
                me.getUsername(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/rooms/" + room.get().getId(), dto);
    }

    @GetMapping("/member/{room_id}")
    public ApiResponseWrapper<List<MemberDTO>> getMembers(Principal principal, @PathVariable("room_id") Long roomId)
    {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        Optional<Room> roomOpt = roomRepo.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ApiResponseWrapper.error("room not found");
        }

        Room room = roomOpt.get();

        boolean isMember = room.getMembers().stream()
                .anyMatch(rm -> rm.getUser().getId().equals(me.getId()));

        if (!isMember) {
            return ApiResponseWrapper.error("you are not a member of this room");
        }

        List<MemberDTO> dto = room.getMembers().stream()
                .map(rm -> new MemberDTO(
                        rm.getUser().getUsername(),
                        rm.getRole()
                ))
                .toList();

        return ApiResponseWrapper.ok(dto);
    }

    @GetMapping("/friends/not-in/{room_id}")
    public ApiResponseWrapper<List<UserDTO>> getFriendsNotInGroup(Principal principal, @PathVariable("room_id") Long roomId){
        Room room = roomRepo.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseWrapper.error("room doesn't exists");
        }
        User me = userRepo.findByUsername(principal.getName());
        List<Friendship> friendships =
                friendshipRepo.findByStatusAndRequester_IdOrStatusAndAddressee_Id(
                        FriendshipStatus.ACCEPTED, me.getId(),
                        FriendshipStatus.ACCEPTED, me.getId()
                );
        List<UserDTO> friends = friendships.stream().map(fr ->
                new UserDTO((fr.getAddressee().getUsername().equals(me.getUsername()))? fr.getRequester().getUsername() :
                        fr.getAddressee().getUsername() )).toList();


        Set<String> memberUsernames = room.getMembers().stream()
                .map(roomMember -> roomMember.getUser().getUsername())
                .collect(Collectors.toSet());

        List<UserDTO> result = friends.stream()
                .filter(friend -> !memberUsernames.contains(friend.getUsername()))
                .toList();

        return ApiResponseWrapper.ok(result);

    }
    @PostMapping ("/invite-token/{room_id}")
    public ApiResponseWrapper<String> generateToken(Principal principal, @PathVariable("room_id") Long roomId){
        Room room = roomRepo.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseWrapper.error("Room doesn't exists");
        }
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("User not found");
        }

        boolean isMember = room.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(me.getId()));

        if (!isMember) {
            return ApiResponseWrapper.error("You are not a member of this room");
        }
        RoomMember member = roomMemberRepo.findByRoomIdAndUserId(room.getId(), me.getId());

        if (member.getRole() != RoomMemberRole.OWNER &&
                member.getRole() != RoomMemberRole.ADMIN) {
            return ApiResponseWrapper.error("You don't have permission to generate invite token");
        }
        String token = UUID.randomUUID().toString();

        room.setInviteToken(token);
        room.setInviteTokenExpiresAt(LocalDateTime.now().plusDays(1));
        roomRepo.save(room);

        String qrValue = "studybuddy://join-room?token=" + token;

        return ApiResponseWrapper.ok(qrValue);

    }
    @PostMapping("/join-by-token")
    public ApiResponseWrapper<String> joinByToken(Principal principal, @RequestBody Map<String,String> api){
        String token = api.get("token");
        if (token == null || token.isBlank() || token.isEmpty()){
            return ApiResponseWrapper.error("null token");
        }
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("User not found");
        }

        Room room = roomRepo.findByInviteToken(token);
        if (room == null) {
            return ApiResponseWrapper.error("Invalid token");
        }

        if (room.getInviteTokenExpiresAt() == null ||
                room.getInviteTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return ApiResponseWrapper.error("Token expired");
        }

        boolean alreadyMember = room.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(me.getId()));

        if (alreadyMember) {
            return ApiResponseWrapper.error("You are already in this room");
        }

        RoomMember roomMember = new RoomMember(room, me, RoomMemberRole.MEMBER);

        room.getMembers().add(roomMember);

        roomRepo.save(room);

        return ApiResponseWrapper.ok("Joined room successfully");
    }



}
