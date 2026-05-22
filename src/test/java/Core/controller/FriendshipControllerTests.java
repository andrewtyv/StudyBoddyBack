package Core.controller;

import DTO.ApiResponse;
import DTO.ApiResponseWrapper;
import DTO.FriendshipDTO;
import controllers.FriendshipController;
import model.Friendship;
import model.FriendshipStatus;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repos.FriendshipRepo;
import repos.UserBlockRepo;
import repos.UserRepo;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendshipControllerTests {

    @InjectMocks
    private FriendshipController friendshipController;

    @Mock
    private UserRepo userRepo;

    @Mock
    private FriendshipRepo friendshipRepo;

    @Mock
    private UserBlockRepo userBlockRepo;

    private Principal principal(String username) {
        return () -> username;
    }



    @Test
    void makeRequest_returnsError_whenRequesterNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);
        when(userRepo.findByUsername("anna")).thenReturn(user(2L, "anna"));

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void makeRequest_returnsError_whenAddresseeNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(user(1L, "john"));
        when(userRepo.findByUsername("anna")).thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void makeRequest_returnsError_whenUserAddsHimself() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "john")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("cannot add yourself", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void makeRequest_returnsError_whenSomeoneIsBlocked() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(true);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("someone is blocked...", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void makeRequest_createsNewFriendship_whenNoExistingFriendship() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(anna, john)).thenReturn(false);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertTrue(booleanField(response, "success"));
        assertEquals("request successfully created", stringField(response, "data"));

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepo).save(captor.capture());

        Friendship saved = captor.getValue();
        assertEquals("john", saved.getRequester().getUsername());
        assertEquals("anna", saved.getAddressee().getUsername());
    }

    @Test
    void makeRequest_reusesRejectedFriendship_andSetsPending() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship rejected = friendship(100L, anna, john, FriendshipStatus.REJECTED);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(anna, john)).thenReturn(false);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(rejected);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertTrue(booleanField(response, "success"));
        assertEquals("request successfully created", stringField(response, "data"));

        assertEquals(FriendshipStatus.PENDING, rejected.getStatus());
        assertEquals(john, rejected.getRequester());
        assertEquals(anna, rejected.getAddressee());

        verify(friendshipRepo).save(rejected);
    }

    @Test
    void makeRequest_returnsError_whenFriendshipAlreadyExists() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship existing = friendship(100L, john, anna, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(anna, john)).thenReturn(false);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(existing);

        ApiResponseWrapper<String> response = friendshipController.makeRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("friendship already exist", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }



    @Test
    void incoming_returnsError_whenUserIsNull() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.incoming(principal("john"));

        assertFalse(booleanField(response, "success"));
        assertEquals("null user", stringField(response, "message"));
    }

    @Test
    void incoming_returnsPendingIncomingRequests() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship request = friendship(10L, anna, john, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByAddressee_IdAndStatus(1L, FriendshipStatus.PENDING))
                .thenReturn(List.of(request));

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.incoming(principal("john"));

        assertTrue(booleanField(response, "success"));

        List<FriendshipDTO> data = field(response, "data");
        assertEquals(1, data.size());

        FriendshipDTO dto = data.get(0);
        assertEquals(10L, longField(dto, "id"));
        assertEquals("anna", stringField(dto, "username"));
        assertEquals("PENDING", stringField(dto, "status"));
    }


    @Test
    void outgoing_returnsError_whenUserIsNull() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.outgoing(principal("john"));

        assertFalse(booleanField(response, "success"));
        assertEquals("null user", stringField(response, "message"));
    }

    @Test
    void outgoing_returnsPendingOutgoingRequests() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship request = friendship(11L, john, anna, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByRequester_IdAndStatus(1L, FriendshipStatus.PENDING))
                .thenReturn(List.of(request));

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.outgoing(principal("john"));

        assertTrue(booleanField(response, "success"));

        List<FriendshipDTO> data = field(response, "data");
        assertEquals(1, data.size());

        FriendshipDTO dto = data.get(0);
        assertEquals(11L, longField(dto, "id"));
        assertEquals("anna", stringField(dto, "username"));
        assertEquals("PENDING", stringField(dto, "status"));
    }



    @Test
    void acceptFriendship_returnsError_whenFriendshipDoesNotExist() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("anna")).thenReturn(anna);
        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByRequester_IdAndAddressee_Id(2L, 1L)).thenReturn(null);

        ApiResponse response = friendshipController.acceptFriendship(
                principal("john"),
                Map.of("requester_username", "anna")
        );

        assertEquals("this friedship never existed", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void acceptFriendship_setsStatusAccepted() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(20L, anna, john, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("anna")).thenReturn(anna);
        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByRequester_IdAndAddressee_Id(2L, 1L)).thenReturn(friendship);

        ApiResponse response = friendshipController.acceptFriendship(
                principal("john"),
                Map.of("requester_username", "anna")
        );

        assertEquals("friendship accepted", stringField(response, "message"));
        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());

        verify(friendshipRepo).save(friendship);
    }



    @Test
    void rejectFriendship_returnsError_whenFriendshipDoesNotExist() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("anna")).thenReturn(anna);
        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByRequester_IdAndAddressee_Id(2L, 1L)).thenReturn(null);

        ApiResponse response = friendshipController.rejectFriendship(
                principal("john"),
                Map.of("requester_username", "anna")
        );

        assertEquals("this friendship neber existed", stringField(response, "message"));

        verify(friendshipRepo, never()).save(any());
    }

    @Test
    void rejectFriendship_setsStatusRejected() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(21L, anna, john, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("anna")).thenReturn(anna);
        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByRequester_IdAndAddressee_Id(2L, 1L)).thenReturn(friendship);

        ApiResponse response = friendshipController.rejectFriendship(
                principal("john"),
                Map.of("requester_username", "anna")
        );

        assertEquals("friendship rejected", stringField(response, "message"));
        assertEquals(FriendshipStatus.REJECTED, friendship.getStatus());

        verify(friendshipRepo).save(friendship);
    }

    @Test
    void friendship_returnsError_whenPrincipalNameIsNull() {
        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.friendship(principal(null));

        assertFalse(booleanField(response, "success"));
        assertEquals("empty username", stringField(response, "message"));

        verify(userRepo, never()).findByUsername(any());
    }

    @Test
    void friendship_returnsAcceptedFriends_whenCurrentUserIsRequester() {
        User john = user(1L, "john", "john-photo.png");
        User anna = user(2L, "anna", "anna-photo.png");

        Friendship friendship = friendship(30L, john, anna, FriendshipStatus.ACCEPTED);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByStatusAndRequester_IdOrStatusAndAddressee_Id(
                FriendshipStatus.ACCEPTED, 1L,
                FriendshipStatus.ACCEPTED, 1L
        )).thenReturn(List.of(friendship));

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.friendship(principal("john"));

        assertTrue(booleanField(response, "success"));

        List<FriendshipDTO> data = field(response, "data");
        assertEquals(1, data.size());

        FriendshipDTO dto = data.get(0);
        assertEquals(30L, longField(dto, "id"));
        assertEquals("anna", stringField(dto, "username"));
        assertEquals("ACCEPTED", stringField(dto, "status"));
    }

    @Test
    void friendship_returnsAcceptedFriends_whenCurrentUserIsAddressee() {
        User john = user(1L, "john", "john-photo.png");
        User anna = user(2L, "anna", "anna-photo.png");

        Friendship friendship = friendship(31L, anna, john, FriendshipStatus.ACCEPTED);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(friendshipRepo.findByStatusAndRequester_IdOrStatusAndAddressee_Id(
                FriendshipStatus.ACCEPTED, 1L,
                FriendshipStatus.ACCEPTED, 1L
        )).thenReturn(List.of(friendship));

        ApiResponseWrapper<List<FriendshipDTO>> response =
                friendshipController.friendship(principal("john"));

        assertTrue(booleanField(response, "success"));

        List<FriendshipDTO> data = field(response, "data");
        assertEquals(1, data.size());

        FriendshipDTO dto = data.get(0);
        assertEquals(31L, longField(dto, "id"));
        assertEquals("anna", stringField(dto, "username"));
        assertEquals("ACCEPTED", stringField(dto, "status"));
    }


    @Test
    void removeFriend_returnsError_whenFriendUsernameMissing() {
        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of()
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("friend username is required", stringField(response, "message"));

        verifyNoInteractions(userRepo);
        verifyNoInteractions(friendshipRepo);
    }

    @Test
    void removeFriend_returnsError_whenFriendUsernameBlank() {
        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of("friend_username", "   ")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("friend username is required", stringField(response, "message"));

        verifyNoInteractions(userRepo);
        verifyNoInteractions(friendshipRepo);
    }

    @Test
    void removeFriend_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(user(1L, "john"));
        when(userRepo.findByUsername("anna")).thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of("friend_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void removeFriend_returnsError_whenFriendshipNotFound() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of("friend_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("friendship not found", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void removeFriend_returnsError_whenFriendshipIsNotAccepted() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(40L, john, anna, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(friendship);

        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of("friend_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("users are not friends", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void removeFriend_deletesAcceptedFriendship() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(41L, john, anna, FriendshipStatus.ACCEPTED);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                1L, 2L, 2L, 1L
        )).thenReturn(friendship);

        ApiResponseWrapper<String> response = friendshipController.removeFriend(
                principal("john"),
                Map.of("friend_username", "anna")
        );

        assertTrue(booleanField(response, "success"));
        assertEquals("friend removed successfully", stringField(response, "data"));

        verify(friendshipRepo).delete(friendship);
    }



    @Test
    void cancelFriendshipRequest_returnsError_whenAddresseeUsernameMissing() {
        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of()
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("invalid usernames", stringField(response, "message"));

        verifyNoInteractions(userRepo);
    }

    @Test
    void cancelFriendshipRequest_returnsError_whenAddresseeUsernameBlank() {
        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of("addressee_username", "   ")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("invalid usernames", stringField(response, "message"));

        verifyNoInteractions(userRepo);
    }

    @Test
    void cancelFriendshipRequest_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(user(1L, "john"));
        when(userRepo.findByUsername("anna")).thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void cancelFriendshipRequest_returnsError_whenRequestNotFound() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_Id(1L, 2L))
                .thenReturn(null);

        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("request not found", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void cancelFriendshipRequest_returnsError_whenRequestIsNotPending() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(50L, john, anna, FriendshipStatus.ACCEPTED);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_Id(1L, 2L))
                .thenReturn(friendship);

        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("request is not pending", stringField(response, "message"));

        verify(friendshipRepo, never()).delete(any());
    }

    @Test
    void cancelFriendshipRequest_deletesPendingRequest() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Friendship friendship = friendship(51L, john, anna, FriendshipStatus.PENDING);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(userRepo.findByUsername("anna")).thenReturn(anna);

        when(friendshipRepo.findByRequester_IdAndAddressee_Id(1L, 2L))
                .thenReturn(friendship);

        ApiResponseWrapper<String> response = friendshipController.cancelFriendshipRequest(
                principal("john"),
                Map.of("addressee_username", "anna")
        );

        assertTrue(booleanField(response, "success"));
        assertEquals("request canceled", stringField(response, "data"));

        verify(friendshipRepo).delete(friendship);
    }



    private User user(Long id, String username) {
        return user(id, username, null);
    }

    private User user(Long id, String username, String photoUrl) {
        User user = new User();
        setField(user, "id", id);
        setField(user, "username", username);
        setField(user, "photoUrl", photoUrl);
        return user;
    }

    private Friendship friendship(Long id, User requester, User addressee, FriendshipStatus status) {
        Friendship friendship = new Friendship(requester, addressee);
        setField(friendship, "id", id);
        setField(friendship, "requester", requester);
        setField(friendship, "addressee", addressee);
        setField(friendship, "status", status);
        setField(friendship, "friendshipSentAt", LocalDateTime.now());
        return friendship;
    }

    private boolean booleanField(Object target, String fieldName) {
        Object value = field(target, fieldName);
        return Boolean.TRUE.equals(value);
    }

    private String stringField(Object target, String fieldName) {
        Object value = field(target, fieldName);
        return value == null ? null : value.toString();
    }

    private long longField(Object target, String fieldName) {
        return ((Number) field(target, fieldName)).longValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read field: " + fieldName, e);
        }
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
}