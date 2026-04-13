package repos;

import model.Blog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogRepo extends JpaRepository<Blog, Long> {
        List<Blog> findAllByOrderByCreatedAtDesc();
        Optional<Blog> findByClientIdAndAuthor_Id(String clientId, Long authorId);
}