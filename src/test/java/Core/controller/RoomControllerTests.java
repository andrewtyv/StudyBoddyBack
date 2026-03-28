package Core.controller;


import DTO.ApiResponseWrapper;
import DTO.RoomDTO;
import controllers.RoomController;
import model.Room;
import model.RoomMember;
import model.RoomType;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Map;

/*
createRoom
create_invite
accept
sendMessage
generateToken
joinByToken
 */
@ExtendWith(MockitoExtension.class)
public class RoomControllerTests {
    @InjectMocks
    private RoomController roomController;

    @Mock
    UserRepo userRepo;

    @Mock
    MessageRepo messageRepo;

    @Mock
    RoomMemberRepo roomMemberRepo;

    @Mock
    RoomRepo roomRepo;

    @Mock
    RoomInviteRepo roomInviteRepo;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    FriendshipRepo friendshipRepo;

    @Mock
    Principal principal;

    @Test
    public void createGroupRoom_ShouldCreateGroup_WhenInputIsValid() {
        // Arrange
        Map<String, String> request = Map.of(
                "name", "testRoom"
        );

        User me = new User("test@gmail.com", "testName", "1234");

        when(principal.getName()).thenReturn("testName");
        when(userRepo.findByUsername("testName")).thenReturn(me);

        // Act
        ApiResponseWrapper<RoomDTO> response = roomController.createGroupRoom(principal, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNull(response.getMessage());

        RoomDTO dto = response.getData();
        assertNotNull(dto);
        assertEquals(RoomType.GROUP, dto.getRoomType());
        assertEquals("testRoom", dto.getMessage());
        assertNull(dto.getDirectKey());


        verify(roomRepo).save(any(Room.class));
        verify(roomMemberRepo).save(any(RoomMember.class));
    }


}
