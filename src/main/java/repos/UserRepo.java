package repos;

import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByUsername(String username);

    User findByEmail(String email);
    User findByEmailOrUsername(String email, String username);

}
