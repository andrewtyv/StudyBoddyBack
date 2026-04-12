package repos;

import model.User;
import model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBlockRepo extends JpaRepository<UserBlock, Long>
{
    boolean existsByBlockedAndBlocker(User blocked, User blocker);


    List<UserBlock> findByBlocker(User user);

    UserBlock findByBlockedAndAndBlocker(User blocked,User blocker);
}
