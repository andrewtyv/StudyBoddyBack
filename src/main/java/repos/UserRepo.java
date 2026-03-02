package repos;

import model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByUsername(String username);

    User findByEmail(String email);
    User findByEmailOrUsername(String email, String username);

}
