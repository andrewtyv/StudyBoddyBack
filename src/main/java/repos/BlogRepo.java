package repos;

import model.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlogRepo extends JpaRepository<Blog, Long> {
        List<Blog> findAllByOrderByCreatedAtDesc();
}
