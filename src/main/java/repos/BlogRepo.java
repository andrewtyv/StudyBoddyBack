package repos;

import model.Blog;
import model.Subject;
import model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface BlogRepo extends JpaRepository<Blog, Long> {

        @Query("""
        SELECT b FROM Blog b
        WHERE b.author.id <> :userId
          AND NOT EXISTS (
              SELECT ub FROM UserBlock ub
              WHERE
                  (ub.blocker.id = :userId AND ub.blocked.id = b.author.id)
                  OR
                  (ub.blocker.id = b.author.id AND ub.blocked.id = :userId)
          )
        ORDER BY
          CASE WHEN b.author.role = :teacherRole THEN 0 ELSE 1 END,
          b.createdAt DESC
    """)
        Page<Blog> findFeedWithoutRecommendations(
                @Param("userId") Long userId,
                @Param("teacherRole") UserRole teacherRole,
                Pageable pageable
        );

        @Query("""
        SELECT b FROM Blog b
        WHERE b.author.id <> :userId
          AND NOT EXISTS (
              SELECT ub FROM UserBlock ub
              WHERE
                  (ub.blocker.id = :userId AND ub.blocked.id = b.author.id)
                  OR
                  (ub.blocker.id = b.author.id AND ub.blocked.id = :userId)
          )
        ORDER BY
          CASE WHEN b.subject IN :subjects THEN 0 ELSE 1 END,
          CASE WHEN b.author.role = :teacherRole THEN 0 ELSE 1 END,
          b.createdAt DESC
    """)
        Page<Blog> findRecommendedFeed(
                @Param("userId") Long userId,
                @Param("subjects") Set<Subject> subjects,
                @Param("teacherRole") UserRole teacherRole,
                Pageable pageable
        );

        @Query("""
        SELECT b FROM Blog b
        WHERE b.author.id <> :userId
          AND b.subject = :subject
          AND NOT EXISTS (
              SELECT ub FROM UserBlock ub
              WHERE
                  (ub.blocker.id = :userId AND ub.blocked.id = b.author.id)
                  OR
                  (ub.blocker.id = b.author.id AND ub.blocked.id = :userId)
          )
        ORDER BY
          CASE WHEN b.author.role = :teacherRole THEN 0 ELSE 1 END,
          b.createdAt DESC
    """)
        Page<Blog> findFeedBySubject(
                @Param("userId") Long userId,
                @Param("subject") Subject subject,
                @Param("teacherRole") UserRole teacherRole,
                Pageable pageable
        );

        Page<Blog> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}