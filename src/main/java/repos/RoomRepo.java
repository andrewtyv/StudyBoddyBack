package repos;

import model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepo extends JpaRepository<Room, Long> {
    Room findByDirectKey(String directKey);

}
