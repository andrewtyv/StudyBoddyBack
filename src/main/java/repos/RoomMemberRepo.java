package repos;

import model.Room;
import model.RoomMember;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMemberRepo extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByUser(User user);

    boolean existsByRoom_IdAndUser_Id(Long roomId,Long userId);

    List<RoomMember> findByRoom_Id(Long id);


}
