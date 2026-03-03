package repos;

import model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepo extends JpaRepository<RoomMember, Long> {

}
