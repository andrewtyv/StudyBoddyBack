package repos;

import model.Friendship;
import model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepo extends JpaRepository<Friendship, Long> {

    boolean existsByRequester_IdAndAddressee_Id(Long requesterId, Long addresseeId);

    Friendship findByRequester_IdAndAddressee_Id(Long reqesterId, Long addresseId);


    List<Friendship> findByAddressee_IdAndStatus(Long id, FriendshipStatus status);

    List<Friendship> findByRequester_IdAndStatus(Long id, FriendshipStatus status);

}
