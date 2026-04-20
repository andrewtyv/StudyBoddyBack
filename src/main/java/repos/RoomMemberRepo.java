package repos;

import model.Room;
import model.RoomMember;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMemberRepo extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByUser(User user);

    boolean existsByRoom_IdAndUser_Id(Long roomId,Long userId);

    List<RoomMember> findByRoom_Id(Long id);

    RoomMember findByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    RoomMember findByRoomIdAndUserId(Long roomId, Long userId);

    @Query("""
       select rm
       from RoomMember rm
       join fetch rm.user
       where rm.room.id = :roomId
       """)
    List<RoomMember> findByRoomIdWithUser(@Param("roomId") Long roomId);

}
