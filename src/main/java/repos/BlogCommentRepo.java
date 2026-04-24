package repos;

import model.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogCommentRepo extends JpaRepository<BlogComment,Long> {

    List<BlogComment> findByBlogId(Long id);

    long countByBlogId(Long id);

}
