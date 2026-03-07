package repos;

import model.Message;
import model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepo extends JpaRepository<Message, Long> {
    List<Message> findByRoom_IdOrderByCreatedAtAsc(Long roomId);}
