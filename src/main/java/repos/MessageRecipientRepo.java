package repos;

import model.MessageRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRecipientRepo extends JpaRepository<MessageRecipient,Long> {

}
