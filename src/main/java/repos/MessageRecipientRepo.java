package repos;

import model.MessageRecipient;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRecipientRepo extends JpaRepository<MessageRecipient,Long> {
    long countByRecipient_IdAndReadFalseAndMessage_Room_Id(Long recipientId, Long roomId);
    List<MessageRecipient> findByRecipientAndRead(User recipient, boolean read);
}
