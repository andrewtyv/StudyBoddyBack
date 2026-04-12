package controllers;

import DTO.*;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import repos.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Controller responsible for room and messaging operations.
 *
 * <p>This controller provides endpoints for creating direct rooms,
 * retrieving rooms of the authenticated user, marking messages as read,
 * loading messages from a room, and sending messages through WebSocket.</p>
 */
@RestController
@RequestMapping("/room")
@Tag(name = "Rooms", description = "Endpoints for direct chats, group rooms, invites, messages, and room membership")
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


    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
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
    @Operation(
            summary = "Create direct room",
            description = "Creates a direct room between the authenticated user and another user. If such room already exists, returns the existing room."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Direct room created or found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "created",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "ok",
                                                      "data": {
                                                        "id": 12,
                                                        "roomType": "DIRECT",
                                                        "directKey": "3:8",
                                                        "title": "",
                                                        "unreadCount": 0
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "already_exists",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "ok",
                                                      "data": {
                                                        "id": 12,
                                                        "title": "the room already created"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "username is not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "cannot create direct room with yourself",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Create group room",
            description = "Creates a new group room and makes the authenticated user its owner."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Group room created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": {
                                                "id": 20,
                                                "roomType": "GROUP",
                                                "directKey": null,
                                                "title": "Math group",
                                                "unreadCount": 0
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User or validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "user not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "group name is required",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Create room invite",
            description = "Creates a pending invite for a user to join a group room. Only room owner or admin can invite users."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invite created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "invite created succesfully"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or permission error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "something is null",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You cannot invite yourself",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Cannot invite users to direct room",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Invite already exists",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You are not a member of this room",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "User is already a member of this room",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You don't have permission to invite users",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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

    @Operation(
            summary = "Get my invites",
            description = "Returns all pending room invites for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pending invites returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "id": 5,
                                                  "roomId": 20,
                                                  "roomName": "Math group",
                                                  "inviterUsername": "teacher1"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Accept room invite",
            description = "Accepts a pending room invite for the authenticated user and adds the user to the room."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invite accepted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "accepted"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invite validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "This is not your invite",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Invite already processed",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Already in room",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Decline room invite",
            description = "Declines a pending room invite for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invite accepted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "accepted"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invite validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "This is not your invite",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Invite already processed",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Already in room",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
        @Operation(
                summary = "Get all rooms",
                description = "Returns all rooms of the authenticated user together with unread message counts."
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Invite declined successfully",
                        content = @Content(
                                mediaType = "application/json",
                                examples = @ExampleObject(
                                        value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "accepted"
                                            }
                                            """
                                )
                        )
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invite validation error",
                        content = @Content(
                                mediaType = "application/json",
                                examples = {
                                        @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "This is not your invite",
                                              "data": null
                                            }
                                            """),
                                        @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Invite already processed",
                                              "data": null
                                            }
                                            """),
                                        @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Already in room",
                                              "data": null
                                            }
                                            """)
                                }
                        )
                )
        })
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
    @Operation(
            summary = "Delete member from group room",
            description = "Removes a user from a group room. Only room owner or admin can delete another member. This operation is not allowed for direct rooms and the authenticated user cannot delete themselves."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Member deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": null,
                                          "data": "Deleted successfully",
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation, permission, or lookup error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "missing_fields",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "room_id and username are required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "room_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "Room not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "direct_room",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "Can't delete member from direct room",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "target_user_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "User to delete not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "delete_yourself",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "You cannot delete yourself",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "not_members",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "You or target user are not members of this group",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "my_membership_missing",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "Your membership was not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "member_cannot_delete",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "Member can't delete another member",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "target_membership_missing",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "Target membership not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "user_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "User not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    @DeleteMapping("/delete-member")
    public ApiResponseWrapper<String> deleteMember(Principal principal, @RequestBody Map<String, String> api) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("User not found");
        }

        Long roomId = Long.parseLong(api.get("room_id"));
        String username = api.get("username");

        if (roomId == null || username == null) {
            return ApiResponseWrapper.error("room_id and username are required");
        }


        Room room = roomRepo.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseWrapper.error("Room not found");
        }

        if (room.getRoomType() == RoomType.DIRECT) {
            return ApiResponseWrapper.error("Can't delete member from direct room");
        }

        User enemy = userRepo.findByUsername(username);
        if (enemy == null) {
            return ApiResponseWrapper.error("User to delete not found");
        }

        if (me.getId().equals(enemy.getId())) {
            return ApiResponseWrapper.error("You cannot delete yourself");
        }

        boolean meInRoom = roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, me.getId());
        boolean targetInRoom = roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, enemy.getId());

        if (!meInRoom || !targetInRoom) {
            return ApiResponseWrapper.error("You or target user are not members of this group");
        }

        RoomMember memberMe = roomMemberRepo.findByRoom_IdAndUser_Id(roomId, me.getId());
        if (memberMe == null) {
            return ApiResponseWrapper.error("Your membership was not found");
        }

        if (memberMe.getRole() == RoomMemberRole.MEMBER) {
            return ApiResponseWrapper.error("Member can't delete another member");
        }

        RoomMember targetMember = roomMemberRepo.findByRoom_IdAndUser_Id(roomId, enemy.getId());
        if (targetMember == null) {
            return ApiResponseWrapper.error("Target membership not found");
        }

        roomMemberRepo.delete(targetMember);
        return ApiResponseWrapper.ok("Deleted successfully");
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
    @Operation(
            summary = "Mark room messages as read",
            description = "Marks all unread messages in the specified room as read for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rooms returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "id": 12,
                                                  "roomType": "DIRECT",
                                                  "directKey": "3:8",
                                                  "title": "john",
                                                  "unreadCount": 2
                                                },
                                                {
                                                  "id": 20,
                                                  "roomType": "GROUP",
                                                  "directKey": null,
                                                  "title": "Math group",
                                                  "unreadCount": 0
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Enter room",
            description = "Returns all messages from the specified room for the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Room messages returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "content": "Hello",
                                                  "messageType": "TEXT",
                                                  "senderUsername": "john"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Room validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "room with provided id not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "you are not a member of this room",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
    @PostMapping("/enter")
    public ApiResponseWrapper<List<MessageDTO>> enterRoom(
            Principal principal,
            @RequestBody Map<String, String> api
    ) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (api == null || api.get("id") == null || api.get("id").isBlank()) {
            return ApiResponseWrapper.error("room id is required");
        }

        Long roomId;
        try {
            roomId = Long.parseLong(api.get("id"));
        } catch (NumberFormatException e) {
            return ApiResponseWrapper.error("invalid room id");
        }

        Optional<Room> room = roomRepo.findById(roomId);
        if (room.isEmpty()) {
            return ApiResponseWrapper.error("room with provided id not found");
        }

        boolean isMember = roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, me.getId());
        if (!isMember) {
            return ApiResponseWrapper.error("you are not a member of this room");
        }

        List<Message> messages = messageRepo.findByRoom_IdOrderByCreatedAtAsc(roomId);
        List<MessageDTO> dto = new ArrayList<>();

        for (Message message : messages) {
            dto.add(new MessageDTO(
                    message.getId(),
                    message.getContent(),
                    message.getMessageType(),
                    message.getSender().getUsername(),
                    message.getCreatedAt(),
                    message.getPhotoUrl(),
                    message.getPhotoName(),
                    message.getPhotoContentType()
            ));
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
     */
    @Operation(
            summary = "Send room message over WebSocket",
            description = "WebSocket endpoint for sending a message to a room. Payload should contain roomId, content, and messageType."
    )
    @MessageMapping("/send-message")
    public void sendMessage(Principal principal, SendMessageRequest req) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return;
        }

        if (req == null || req.getRoomId() == null || req.getMessageType() == null) {
            return;
        }

        Optional<Room> roomOpt = roomRepo.findById(req.getRoomId());
        if (roomOpt.isEmpty()) {
            return;
        }

        if (!roomMemberRepo.existsByRoom_IdAndUser_Id(req.getRoomId(), me.getId())) {
            return;
        }

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(req.getMessageType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        Room room = roomOpt.get();

        Message message = new Message();
        message.setUser(me);
        message.setRoom(room);
        message.setMessageType(messageType);

        if (messageType == MessageType.TEXT) {
            if (req.getContent() == null || req.getContent().isBlank()) {
                return;
            }
            message.setContent(req.getContent().trim());
        } else if (messageType == MessageType.PHOTO) {
            if (req.getContent() == null || req.getContent().isBlank()) {
                return;
            }

            message.setContent("[PHOTO]");
            message.setPhotoUrl(req.getContent().trim());
            message.setPhotoName(req.getFileName());
            message.setPhotoContentType(req.getContentType());
        } else {
            return;
        }

        messageRepo.save(message);

        List<RoomMember> members = roomMemberRepo.findByRoom_Id(room.getId());
        List<MessageRecipient> recipients = new ArrayList<>();

        for (RoomMember member : members) {
            if (!member.getUser().getId().equals(me.getId())) {
                recipients.add(new MessageRecipient(member.getUser(), message));
                sendExpoPush(member.getUser(), me, room, message);
            }
        }

        if (!recipients.isEmpty()) {
            messageRecipientRepo.saveAll(recipients);
        }
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                message.getCreatedAt(),
                ZoneId.systemDefault()
        );

        MessageWsDTO dto = new MessageWsDTO(
                message.getId(),
                room.getId(),
                me.getUsername(),
                message.getContent(),
                message.getMessageType(),
                createdAt,
                message.getPhotoUrl(),
                message.getPhotoContentType(),
                message.getPhotoName()
        );

        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), dto);
    }
    @Operation(
            summary = "Upload room photo",
            description = "Uploads an image file for a room message. Only room members can upload. Allowed formats: jpg, jpeg, png, webp."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Photo uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "fileUrl": "/uploads/chat/room_12/abc123.jpg",
                                          "fileName": "photo.jpg",
                                          "contentType": "image/jpeg"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                        "File is empty"
                                        """),
                                    @ExampleObject(value = """
                                        "User not found"
                                        """),
                                    @ExampleObject(value = """
                                        "Only jpg, jpeg, png, webp are allowed"
                                        """),
                                    @ExampleObject(value = """
                                        "Room not found"
                                        """),
                                    @ExampleObject(value = """
                                        "Upload failed"
                                        """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is not a member of the room",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    value = "Not a member of this room"
                            )
                    )
            )
    })
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(Principal principal, @RequestParam("file") MultipartFile file, @RequestParam("roomId") Long roomId){
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body("Only jpg, jpeg, png, webp are allowed");
        }
        Optional<Room> roomOpt = roomRepo.findById(roomId);

        if (roomOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Room not found");
        }
        if (!roomMemberRepo.existsByRoom_IdAndUser_Id(roomId, me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not a member of this room");
        }


        try {
            Path chatUploadPath = Paths.get(uploadDir, "chat","room_"+roomId);
            Files.createDirectories(chatUploadPath);

            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "photo.jpg" : file.getOriginalFilename()
            );

            String extension = getExtension(originalName, contentType);
            String storedName = UUID.randomUUID() + extension;

            Path targetPath = chatUploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = "/uploads/chat/room_" + roomId + "/" + storedName;

            UploadPhotoResponse response = new UploadPhotoResponse(
                    relativeUrl,
                    originalName,
                    contentType
            );

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }

    private String getExtension(String originalName, String contentType) {
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            return originalName.substring(dotIndex);
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    @Operation(
            summary = "Get room members",
            description = "Returns all members of the specified room. Accessible only to room members."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Room members returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "username": "nazar",
                                                  "role": "OWNER"
                                                },
                                                {
                                                  "username": "john",
                                                  "role": "MEMBER"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Room access error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "user not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "room not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "you are not a member of this room",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Get friends not in room",
            description = "Returns the authenticated user's friends who are not already members of the specified room."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Users returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "username": "anna"
                                                },
                                                {
                                                  "username": "maria"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Room error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "room doesn't exists",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Generate room invite token",
            description = "Generates an invite token for a group room. Only owner or admin can generate it."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invite token generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "studybuddy://join-room?token=550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Permission or room error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Room doesn't exists",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "User not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You are not a member of this room",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You don't have permission to generate invite token",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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
    @Operation(
            summary = "Generate room invite token",
            description = "Generates an invite token for a group room. Only owner or admin can generate it."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invite token generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": "studybuddy://join-room?token=550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Permission or room error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "Room doesn't exists",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "User not found",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You are not a member of this room",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "You don't have permission to generate invite token",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            )
    })
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

    private void sendExpoPush(User recipient, User sender, Room room, Message message) {
        try {
            if (recipient == null) {
                return;
            }

            if (recipient.getExpoPushToken() == null || recipient.getExpoPushToken().isBlank()) {
                return;
            }

            if (recipient.getPushNotificationsEnabled() != null && !recipient.getPushNotificationsEnabled()) {
                return;
            }

            String bodyText;
            if (message.getMessageType() == MessageType.PHOTO) {
                bodyText = sender.getUsername() + " sent a photo";
            } else {
                String content = message.getContent() == null ? "" : message.getContent().trim();
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                bodyText = sender.getUsername() + ": " + content;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "chat_message");
            data.put("roomId", room.getId());
            data.put("senderUsername", sender.getUsername());

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", recipient.getExpoPushToken());
            payload.put("title", "StudyBuddy");
            payload.put("body", bodyText);
            payload.put("sound", "default");
            payload.put("data", data);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(
                    "https://exp.host/--/api/v2/push/send",
                    request,
                    String.class
            );

        } catch (Exception e) {
            System.out.println("PUSH SEND ERROR: " + e.getMessage());
        }
    }

}
