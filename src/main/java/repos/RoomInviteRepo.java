package repos;

import model.RoomInvite;
import model.RoomInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomInviteRepo extends JpaRepository<RoomInvite, Long> {
    boolean existsByInviteeIdAndRoomId(Long inviteeId, Long roomId);

    boolean existsByInviteeIdAndRoomIdAndStatus(Long inviteeId, Long roomId, RoomInviteStatus status);

    List<RoomInvite> findAllByInviteeIdAndStatus(Long inviteeId, RoomInviteStatus status);
}
