package repos;

import model.StudentProfile;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepo extends JpaRepository<StudentProfile, Long> {
    StudentProfile findByUser(User me);

}
