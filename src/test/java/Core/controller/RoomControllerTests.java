package Core.controller;

import DTO.*;
import controllers.RoomController;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import repos.*;
import security.WebSocketRoomPresenceTracker;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomControllerTests {

    @InjectMocks
    private RoomController roomController;

    @Mock private UserRepo userRepo;
    @Mock private MessageRepo messageRepo;
    @Mock private MessageRecipientRepo messageRecipientRepo;
    @Mock private RoomMemberRepo roomMemberRepo;
    @Mock private RoomRepo roomRepo;
    @Mock private RoomInviteRepo roomInviteRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private FriendshipRepo friendshipRepo;
    @Mock private UserBlockRepo userBlockRepo;
    @Mock private WebSocketRoomPresenceTracker roomPresenceTracker;
    @Mock private Principal principal;

    @TempDir
    Path tempDir;


    private void authAs(String username) {
        when(principal.getName()).thenReturn(username);
    }

    private User user(Long id, String username) {
        User user = new User(username + "@gmail.com", username, "1234");
        setField(user, "id", id);
        return user;
    }

    private Room groupRoom(Long id, String name) {
        Room room = new Room(RoomType.GROUP, null);
        setField(room, "id", id);
        room.setRoomName(name);
        ensureMembers(room);
        return room;
    }

    private Room directRoom(Long id, String directKey) {
        Room room = new Room(RoomType.DIRECT, directKey);
        setField(room, "id", id);
        ensureMembers(room);
        return room;
    }

    private RoomMember member(Room room, User user, RoomMemberRole role) {
        RoomMember member = new RoomMember(room, user, role);
        ensureMembers(room).add(member);
        return member;
    }

    @SuppressWarnings("unchecked")
    private Set<RoomMember> ensureMembers(Room room) {
        Set<RoomMember> members = room.getMembers();

        if (members == null) {
            members = new HashSet<>();
            setField(room, "members", members);
        }

        return members;
    }

    private RoomInvite invite(Long id, Room room, User inviter, User invitee) {
        RoomInvite invite = new RoomInvite(room, inviter, invitee);
        setField(invite, "id", id);
        return invite;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }


    @Test
    public void createGroupRoom_ShouldCreateGroup_WhenInputIsValid() {
        authAs("me");
        User me = user(1L, "me");

        when(userRepo.findByUsername("me")).thenReturn(me);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createGroupRoom(principal, Map.of("name", " testRoom "));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(RoomType.GROUP, response.getData().getRoomType());
        assertNull(response.getData().getDirectKey());

        verify(roomRepo).save(any(Room.class));
        verify(roomMemberRepo).save(any(RoomMember.class));
    }

    @Test
    public void createGroupRoom_ShouldReturnError_WhenUserNotFound() {
        authAs("me");
        when(userRepo.findByUsername("me")).thenReturn(null);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createGroupRoom(principal, Map.of("name", "room"));

        assertFalse(response.isSuccess());
        assertEquals("user not found", response.getMessage());

        verify(roomRepo, never()).save(any());
        verify(roomMemberRepo, never()).save(any());
    }

    @Test
    public void createGroupRoom_ShouldReturnError_WhenNameBlank() {
        authAs("me");
        when(userRepo.findByUsername("me")).thenReturn(user(1L, "me"));

        ApiResponseWrapper<RoomDTO> response =
                roomController.createGroupRoom(principal, Map.of("name", "   "));

        assertFalse(response.isSuccess());
        assertEquals("group name is required", response.getMessage());

        verify(roomRepo, never()).save(any());
    }


    @Test
    public void createRoom_ShouldCreateDirectRoom_WhenValid() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userBlockRepo.existsByBlockedAndBlocker(any(User.class), any(User.class))).thenReturn(false);
        when(roomRepo.findByDirectKey("1:2")).thenReturn(null);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createRoom(principal, Map.of("username", "friend"));

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(RoomType.DIRECT, response.getData().getRoomType());
        assertEquals("1:2", response.getData().getDirectKey());

        verify(roomRepo).save(any(Room.class));
        verify(roomMemberRepo, times(2)).save(any(RoomMember.class));
    }

    @Test
    public void createRoom_ShouldReturnError_WhenFriendNotFound() {
        authAs("me");

        when(userRepo.findByUsername("friend")).thenReturn(null);
        when(userRepo.findByUsername("me")).thenReturn(user(1L, "me"));

        ApiResponseWrapper<RoomDTO> response =
                roomController.createRoom(principal, Map.of("username", "friend"));

        assertFalse(response.isSuccess());
        assertEquals("username is not found", response.getMessage());

        verify(roomRepo, never()).save(any());
    }

    @Test
    public void createRoom_ShouldReturnError_WhenCreatingWithYourself() {
        authAs("me");

        User me = user(1L, "me");

        when(userRepo.findByUsername("me")).thenReturn(me);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createRoom(principal, Map.of("username", "me"));

        assertFalse(response.isSuccess());
        assertEquals("cannot create direct room with yourself", response.getMessage());

        verify(roomRepo, never()).save(any());
    }

    @Test
    public void createRoom_ShouldReturnError_WhenBlocked() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userBlockRepo.existsByBlockedAndBlocker(me, friend)).thenReturn(true);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createRoom(principal, Map.of("username", "friend"));

        assertFalse(response.isSuccess());
        assertEquals("block error", response.getMessage());

        verify(roomRepo, never()).save(any());
    }

    @Test
    public void createRoom_ShouldReturnExistingRoom_WhenDirectAlreadyExists() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room existingRoom = directRoom(10L, "1:2");

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findByDirectKey("1:2")).thenReturn(existingRoom);

        ApiResponseWrapper<RoomDTO> response =
                roomController.createRoom(principal, Map.of("username", "friend"));

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());

        verify(roomRepo, never()).save(any());
        verify(roomMemberRepo, never()).save(any());
    }


    @Test
    public void createInvite_ShouldCreateInvite_WhenOwnerInvitesFriend() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(100L, "group");
        RoomMember owner = member(room, me, RoomMemberRole.OWNER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findById(100L)).thenReturn(Optional.of(room));
        when(roomInviteRepo.existsByInviteeIdAndRoomIdAndStatus(2L, 100L, RoomInviteStatus.PENDING)).thenReturn(false);
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 1L)).thenReturn(true);
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 2L)).thenReturn(false);
        when(roomMemberRepo.findByRoomIdAndUserId(100L, 1L)).thenReturn(owner);

        ApiResponseWrapper<String> response =
                roomController.create_invite(principal, Map.of("username", "friend", "id", "100"));

        assertTrue(response.isSuccess());
        assertEquals("invite created succesfully", response.getData());

        verify(roomInviteRepo).save(any(RoomInvite.class));
    }

    @Test
    public void createInvite_ShouldReturnError_WhenInviteAlreadyExists() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(100L, "group");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findById(100L)).thenReturn(Optional.of(room));
        when(roomInviteRepo.existsByInviteeIdAndRoomIdAndStatus(2L, 100L, RoomInviteStatus.PENDING)).thenReturn(true);

        ApiResponseWrapper<String> response =
                roomController.create_invite(principal, Map.of("username", "friend", "id", "100"));

        assertFalse(response.isSuccess());
        assertEquals("Invite already exists", response.getMessage());

        verify(roomInviteRepo, never()).save(any());
    }

    @Test
    public void createInvite_ShouldReturnError_WhenUserIsNotOwnerOrAdmin() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(100L, "group");
        RoomMember normalMember = member(room, me, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findById(100L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 1L)).thenReturn(true);
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 2L)).thenReturn(false);
        when(roomMemberRepo.findByRoomIdAndUserId(100L, 1L)).thenReturn(normalMember);

        ApiResponseWrapper<String> response =
                roomController.create_invite(principal, Map.of("username", "friend", "id", "100"));

        assertFalse(response.isSuccess());
        assertEquals("You don't have permission to invite users", response.getMessage());

        verify(roomInviteRepo, never()).save(any());
    }


    @Test
    public void accept_ShouldAcceptInvite_WhenValid() {
        authAs("friend");

        User inviter = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(100L, "group");
        RoomInvite invite = invite(55L, room, inviter, friend);

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomInviteRepo.findById(55L)).thenReturn(Optional.of(invite));
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 2L)).thenReturn(false);

        ApiResponseWrapper<String> response =
                roomController.accept(principal, Map.of("inviteId", "55"));

        assertTrue(response.isSuccess());
        assertEquals("accepted", response.getData());

        verify(roomInviteRepo).save(invite);
        verify(roomMemberRepo).save(any(RoomMember.class));
    }

    @Test
    public void accept_ShouldReturnError_WhenInviteBelongsToAnotherUser() {
        authAs("wrongUser");

        User inviter = user(1L, "me");
        User invitee = user(2L, "friend");
        User wrongUser = user(3L, "wrongUser");
        Room room = groupRoom(100L, "group");
        RoomInvite invite = invite(55L, room, inviter, invitee);

        when(userRepo.findByUsername("wrongUser")).thenReturn(wrongUser);
        when(roomInviteRepo.findById(55L)).thenReturn(Optional.of(invite));

        ApiResponseWrapper<String> response =
                roomController.accept(principal, Map.of("inviteId", "55"));

        assertFalse(response.isSuccess());
        assertEquals("This is not your invite", response.getMessage());

        verify(roomMemberRepo, never()).save(any());
    }

    @Test
    public void decline_ShouldDeclineInvite_WhenValid() {
        authAs("friend");

        User inviter = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(100L, "group");
        RoomInvite invite = invite(55L, room, inviter, friend);

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomInviteRepo.findById(55L)).thenReturn(Optional.of(invite));
        when(roomMemberRepo.existsByRoomIdAndUserId(100L, 2L)).thenReturn(false);

        ApiResponseWrapper<String> response =
                roomController.decline(principal, Map.of("inviteId", "55"));

        assertTrue(response.isSuccess());
        assertEquals("decline", response.getData());

        verify(roomInviteRepo).save(invite);
    }


    @Test
    public void getAllRooms_ShouldReturnDirectAndGroupRooms() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");

        Room direct = directRoom(10L, "1:2");
        member(direct, me, RoomMemberRole.OWNER);
        member(direct, friend, RoomMemberRole.OWNER);

        Room group = groupRoom(20L, "group");
        RoomMember groupMember = member(group, me, RoomMemberRole.OWNER);

        RoomMember directMember = direct.getMembers().stream()
                .filter(rm -> rm.getUser().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomMemberRepo.findByUser(me)).thenReturn(List.of(directMember, groupMember));
        when(messageRecipientRepo.countByRecipient_IdAndReadFalseAndMessage_Room_Id(1L, 10L)).thenReturn(2L);
        when(messageRecipientRepo.countByRecipient_IdAndReadFalseAndMessage_Room_Id(1L, 20L)).thenReturn(0L);

        ApiResponseWrapper<List<RoomDTO>> response = roomController.getAllRooms(principal);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
    }

    @Test
    public void getAllRooms_ShouldReturnError_WhenUserNotFound() {
        authAs("me");

        when(userRepo.findByUsername("me")).thenReturn(null);

        ApiResponseWrapper<List<RoomDTO>> response = roomController.getAllRooms(principal);

        assertFalse(response.isSuccess());
        assertEquals("user not found", response.getMessage());
    }


    @Test
    public void enterRoom_ShouldReturnMessages_WhenUserIsMember() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        Message message = new Message();
        setField(message, "id", 99L);
        message.setUser(me);
        message.setRoom(room);
        message.setContent("hello");
        message.setMessageType(MessageType.TEXT);
        setField(message, "createdAt", Instant.now());

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(messageRepo.findByRoom_IdOrderByCreatedAtAsc(10L)).thenReturn(List.of(message));

        ApiResponseWrapper<List<MessageDTO>> response =
                roomController.enterRoom(principal, Map.of("id", "10"));

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
    }

    @Test
    public void enterRoom_ShouldReturnError_WhenRoomIdInvalid() {
        authAs("me");

        when(userRepo.findByUsername("me")).thenReturn(user(1L, "me"));

        ApiResponseWrapper<List<MessageDTO>> response =
                roomController.enterRoom(principal, Map.of("id", "abc"));

        assertFalse(response.isSuccess());
        assertEquals("invalid room id", response.getMessage());
    }

    @Test
    public void enterRoom_ShouldReturnError_WhenNotMember() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<List<MessageDTO>> response =
                roomController.enterRoom(principal, Map.of("id", "10"));

        assertFalse(response.isSuccess());
        assertEquals("you are not a member of this room", response.getMessage());
    }


    @Test
    public void readMessages_ShouldMarkRecipientsAsRead_WhenValid() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        Message message = new Message();
        message.setRoom(room);
        message.setUser(user(2L, "friend"));
        message.setContent("hello");
        message.setMessageType(MessageType.TEXT);
        setField(message, "createdAt", Instant.now());

        MessageRecipient recipient = new MessageRecipient(me, message);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(messageRecipientRepo.findByRecipientAndRead(me, false)).thenReturn(List.of(recipient));

        ApiResponseWrapper<String> response =
                roomController.ReadMessages(principal, Map.of("id", "10"));

        assertTrue(response.isSuccess());
        assertEquals("read", response.getData());

        verify(messageRecipientRepo).save(recipient);
    }

    @Test
    public void readMessages_ShouldReturnError_WhenNotMember() {
        authAs("me");

        User me = user(1L, "me");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(groupRoom(10L, "group")));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<String> response =
                roomController.ReadMessages(principal, Map.of("id", "10"));

        assertFalse(response.isSuccess());
        assertEquals("you are not a member of this room", response.getMessage());
    }


    @Test
    public void sendMessage_ShouldSaveRecipientsAndBroadcast_WhenTextMessageIsValid() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");

        RoomMember meMember = member(room, me, RoomMemberRole.OWNER);
        RoomMember friendMember = member(room, friend, RoomMemberRole.MEMBER);

        SendMessageRequest req = new SendMessageRequest();
        setField(req, "roomId", 10L);
        setField(req, "messageType", "TEXT");
        setField(req, "content", " hello ");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.findByRoomIdWithUser(10L)).thenReturn(List.of(meMember, friendMember));
        when(roomPresenceTracker.isUserInRoom(10L, "friend")).thenReturn(true);

        when(messageRepo.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            setField(saved, "id", 77L);
            setField(saved, "createdAt", Instant.now());
            return saved;
        });

        roomController.sendMessage(principal, req);

        verify(messageRepo).save(any(Message.class));
        verify(messageRecipientRepo).saveAll(anyList());
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/10"), any(MessageWsDTO.class));
    }

    @Test
    public void sendMessage_ShouldReturnWithoutSaving_WhenUserNotFound() {
        authAs("me");

        SendMessageRequest req = new SendMessageRequest();
        setField(req, "roomId", 10L);
        setField(req, "messageType", "TEXT");
        setField(req, "content", "hello");

        when(userRepo.findByUsername("me")).thenReturn(null);

        roomController.sendMessage(principal, req);

        verify(messageRepo, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageWsDTO.class));
    }

    @Test
    public void sendMessage_ShouldReturnWithoutSaving_WhenTextIsBlank() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        SendMessageRequest req = new SendMessageRequest();
        setField(req, "roomId", 10L);
        setField(req, "messageType", "TEXT");
        setField(req, "content", "   ");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);

        roomController.sendMessage(principal, req);

        verify(messageRepo, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageWsDTO.class));
    }

    @Test
    public void sendMessage_ShouldSavePhotoMessage_WhenPhotoMessageIsValid() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        RoomMember meMember = member(room, me, RoomMemberRole.OWNER);

        SendMessageRequest req = new SendMessageRequest();
        setField(req, "roomId", 10L);
        setField(req, "messageType", "PHOTO");
        setField(req, "content", "/uploads/chat/photo.jpg");
        setField(req, "fileName", "photo.jpg");
        setField(req, "contentType", "image/jpeg");

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.findByRoomIdWithUser(10L)).thenReturn(List.of(meMember));

        when(messageRepo.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            setField(saved, "id", 77L);
            setField(saved, "createdAt", Instant.now());
            return saved;
        });

        roomController.sendMessage(principal, req);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepo).save(captor.capture());

        assertEquals(MessageType.PHOTO, captor.getValue().getMessageType());
        assertEquals("[PHOTO]", captor.getValue().getContent());

        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/10"), any(MessageWsDTO.class));
    }


    @Test
    public void uploadPhoto_ShouldReturnOk_WhenFileIsValid() {
        authAs("me");

        setField(roomController, "uploadDir", tempDir.toString());

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                "fake-image".getBytes()
        );

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);

        ResponseEntity<?> response = roomController.uploadPhoto(principal, file, 10L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    public void uploadPhoto_ShouldReturnBadRequest_WhenContentTypeInvalid() {
        authAs("me");

        User me = user(1L, "me");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "file.txt",
                "text/plain",
                "hello".getBytes()
        );

        when(userRepo.findByUsername("me")).thenReturn(me);

        ResponseEntity<?> response = roomController.uploadPhoto(principal, file, 10L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Only jpg, jpeg, png, webp are allowed", response.getBody());
    }

    @Test
    public void uploadPhoto_ShouldReturnForbidden_WhenUserIsNotRoomMember() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(false);

        ResponseEntity<?> response = roomController.uploadPhoto(principal, file, 10L);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Not a member of this room", response.getBody());
    }


    @Test
    public void getMembers_ShouldReturnMembers_WhenUserIsMember() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");

        member(room, me, RoomMemberRole.OWNER);
        member(room, friend, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));

        ApiResponseWrapper<List<MemberDTO>> response =
                roomController.getMembers(principal, 10L);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
    }

    @Test
    public void getMembers_ShouldReturnError_WhenUserIsNotMember() {
        authAs("stranger");

        User me = user(1L, "stranger");
        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");

        member(room, friend, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("stranger")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));

        ApiResponseWrapper<List<MemberDTO>> response =
                roomController.getMembers(principal, 10L);

        assertFalse(response.isSuccess());
        assertEquals("you are not a member of this room", response.getMessage());
    }


    @Test
    public void generateToken_ShouldGenerateToken_WhenUserIsOwner() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        RoomMember owner = member(room, me, RoomMemberRole.OWNER);

        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomMemberRepo.findByRoomIdAndUserId(10L, 1L)).thenReturn(owner);

        ApiResponseWrapper<String> response = roomController.generateToken(principal, 10L);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().startsWith("studybuddy://join-room?token="));

        verify(roomRepo).save(room);
    }

    @Test
    public void generateToken_ShouldReturnError_WhenNotOwnerOrAdmin() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        RoomMember normalMember = member(room, me, RoomMemberRole.MEMBER);

        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomMemberRepo.findByRoomIdAndUserId(10L, 1L)).thenReturn(normalMember);

        ApiResponseWrapper<String> response = roomController.generateToken(principal, 10L);

        assertFalse(response.isSuccess());
        assertEquals("You don't have permission to generate invite token", response.getMessage());

        verify(roomRepo, never()).save(room);
    }

    @Test
    public void joinByToken_ShouldJoinRoom_WhenTokenIsValid() {
        authAs("friend");

        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");
        room.setInviteToken("abc");
        room.setInviteTokenExpiresAt(LocalDateTime.now().plusHours(1));

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findByInviteToken("abc")).thenReturn(room);

        ApiResponseWrapper<String> response =
                roomController.joinByToken(principal, Map.of("token", "abc"));

        assertTrue(response.isSuccess());
        assertEquals("Joined room successfully", response.getData());

        verify(roomRepo).save(room);
    }

    @Test
    public void joinByToken_ShouldReturnError_WhenTokenExpired() {
        authAs("friend");

        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");
        room.setInviteToken("abc");
        room.setInviteTokenExpiresAt(LocalDateTime.now().minusHours(1));

        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findByInviteToken("abc")).thenReturn(room);

        ApiResponseWrapper<String> response =
                roomController.joinByToken(principal, Map.of("token", "abc"));

        assertFalse(response.isSuccess());
        assertEquals("Token expired", response.getMessage());

        verify(roomRepo, never()).save(room);
    }

    @Test
    public void joinByToken_ShouldReturnError_WhenAlreadyMember() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        room.setInviteToken("abc");
        room.setInviteTokenExpiresAt(LocalDateTime.now().plusHours(1));
        member(room, me, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findByInviteToken("abc")).thenReturn(room);

        ApiResponseWrapper<String> response =
                roomController.joinByToken(principal, Map.of("token", "abc"));

        assertFalse(response.isSuccess());
        assertEquals("You are already in this room", response.getMessage());

        verify(roomRepo, never()).save(room);
    }


    @Test
    public void deleteRoom_ShouldDeleteRoom_WhenUserIsOwner() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        RoomMember owner = member(room, me, RoomMemberRole.OWNER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(owner);

        ApiResponseWrapper<String> response =
                roomController.deleteRoom(principal, 10L);

        assertTrue(response.isSuccess());
        assertEquals("deleted succesfully", response.getData());

        verify(roomRepo).delete(room);
    }

    @Test
    public void deleteRoom_ShouldReturnError_WhenUserIsNotOwner() {
        authAs("me");

        User me = user(1L, "me");
        Room room = groupRoom(10L, "group");
        RoomMember normalMember = member(room, me, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(normalMember);

        ApiResponseWrapper<String> response =
                roomController.deleteRoom(principal, 10L);

        assertFalse(response.isSuccess());
        assertEquals("only owner can delete the group", response.getMessage());

        verify(roomRepo, never()).delete(any());
    }


    @Test
    public void deleteMember_ShouldDeleteTarget_WhenRequesterIsOwner() {
        authAs("me");

        User me = user(1L, "me");
        User enemy = user(2L, "enemy");
        Room room = groupRoom(10L, "group");

        RoomMember owner = member(room, me, RoomMemberRole.OWNER);
        RoomMember target = member(room, enemy, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(owner);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 2L)).thenReturn(target);

        ApiResponseWrapper<String> response =
                roomController.deleteMember(principal, Map.of("room_id", "10", "username", "enemy"));

        assertTrue(response.isSuccess());
        assertEquals("Deleted successfully", response.getData());

        verify(roomMemberRepo).delete(target);
    }

    @Test
    public void deleteMember_ShouldReturnError_WhenRequesterIsNormalMember() {
        authAs("me");

        User me = user(1L, "me");
        User enemy = user(2L, "enemy");
        Room room = groupRoom(10L, "group");

        RoomMember requester = member(room, me, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("enemy")).thenReturn(enemy);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(roomMemberRepo.existsByRoom_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(requester);

        ApiResponseWrapper<String> response =
                roomController.deleteMember(principal, Map.of("room_id", "10", "username", "enemy"));

        assertFalse(response.isSuccess());
        assertEquals("Member can't delete another member", response.getMessage());

        verify(roomMemberRepo, never()).delete(any());
    }


    @Test
    public void grantRole_ShouldGrantAdmin_WhenRequesterIsOwner() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");

        RoomMember owner = member(room, me, RoomMemberRole.OWNER);
        RoomMember target = member(room, friend, RoomMemberRole.MEMBER);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(owner);
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 2L)).thenReturn(target);

        ApiResponseWrapper<String> response =
                roomController.grantRole(principal, Map.of(
                        "room_id", "10",
                        "username", "friend",
                        "role", "ADMIN"
                ));

        assertTrue(response.isSuccess());
        assertEquals("new role was set", response.getData());
        assertEquals(RoomMemberRole.ADMIN, target.getRole());

        verify(roomMemberRepo).save(target);
    }

    @Test
    public void grantRole_ShouldReturnError_WhenRequesterIsNotOwner() {
        authAs("me");

        User me = user(1L, "me");
        User friend = user(2L, "friend");
        Room room = groupRoom(10L, "group");

        RoomMember requester = member(room, me, RoomMemberRole.ADMIN);

        when(userRepo.findByUsername("me")).thenReturn(me);
        when(userRepo.findByUsername("friend")).thenReturn(friend);
        when(roomRepo.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepo.findByRoom_IdAndUser_Id(10L, 1L)).thenReturn(requester);

        ApiResponseWrapper<String> response =
                roomController.grantRole(principal, Map.of(
                        "room_id", "10",
                        "username", "friend",
                        "role", "MEMBER"
                ));

        assertFalse(response.isSuccess());
        assertEquals("Only Owner can change the roles", response.getMessage());

        verify(roomMemberRepo, never()).save(any());
    }
}