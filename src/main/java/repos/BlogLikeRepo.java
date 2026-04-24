package repos;

import model.Blog;
import model.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogLikeRepo extends JpaRepository<BlogLike,Long> {
    boolean existsByBlogIdAndUserId(Long blogId, Long userId);
    BlogLike findByBlogIdAndUserId(Long blogId, Long userId);

    long countByBlogId(Long blogId);

}
