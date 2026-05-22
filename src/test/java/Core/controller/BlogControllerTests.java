package Core.controller;

import DTO.*;
import controllers.BlogController;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import repos.*;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BlogControllerTests {

    @InjectMocks
    private BlogController blogController;

    @Mock
    private UserRepo userRepo;

    @Mock
    private BlogRepo blogRepo;

    @Mock
    private BlogLikeRepo blogLikeRepo;

    @Mock
    private BlogCommentRepo blogCommentRepo;

    @Mock
    private UserBlockRepo userBlockRepo;

    @Mock
    private StudentProfileRepo studentProfileRepo;

    private Principal principal(String username) {
        return () -> username;
    }



    @Test
    void createBlog_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "Title", "Content", subject(), null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_returnsError_whenTitleIsNull() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, null, "Content", subject(), null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("title is required", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_returnsError_whenTitleIsBlank() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "   ", "Content", subject(), null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("title is required", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_returnsError_whenContentIsNull() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "Title", null, subject(), null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("content is required", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_returnsError_whenContentIsBlank() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "Title", "   ", subject(), null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("content is required", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_returnsError_whenSubjectIsNull() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "Title", "Content", null, null)
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("subject is required", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void createBlog_createsBlogSuccessfully() {
        User john = user(1L, "john");
        Subject subject = subject();

        when(userRepo.findByUsername("john")).thenReturn(john);

        doAnswer(invocation -> {
            Blog saved = invocation.getArgument(0);
            setField(saved, "id", 10L);
            setField(saved, "createdAt", Instant.parse("2026-04-13T12:00:00Z"));
            setField(saved, "updatedAt", Instant.parse("2026-04-13T12:00:00Z"));
            return saved;
        }).when(blogRepo).save(any(Blog.class));

        when(blogLikeRepo.countByBlogId(10L)).thenReturn(0L);
        when(blogCommentRepo.countByBlogId(10L)).thenReturn(0L);
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<BlogDTO> response = blogController.createBlog(
                principal("john"),
                blogDto(null, "  My title  ", "  My content  ", subject, null)
        );

        assertTrue(booleanField(response, "success"));

        BlogDTO data = field(response, "data");
        assertEquals(10L, longField(data, "id"));
        assertEquals("My title", data.getTitle());
        assertEquals("My content", data.getContent());
        assertEquals(subject, data.getSubject());

        ArgumentCaptor<Blog> captor = ArgumentCaptor.forClass(Blog.class);
        verify(blogRepo).save(captor.capture());

        Blog savedBlog = captor.getValue();
        assertEquals("My title", savedBlog.getTitle());
        assertEquals("My content", savedBlog.getContent());
        assertEquals(subject, savedBlog.getSubject());
        assertEquals(john, savedBlog.getAuthor());
    }



    @Test
    void getAllBlogs_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getAllBlogs(principal("john"), 0, 10, null);

        assertFalse(booleanField(response, "success"));
        assertEquals("u dont have permission", stringField(response, "message"));
    }

    @Test
    void getAllBlogs_usesSubjectFilter_whenSubjectProvided() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Subject subject = subject();

        Blog blog = blog(100L, "Title", "Content", subject, anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(studentProfileRepo.findByUser(john)).thenReturn(null);

        when(blogRepo.findFeedBySubject(eq(1L), eq(subject), eq(UserRole.TEACHER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog)));

        when(blogLikeRepo.countByBlogId(100L)).thenReturn(5L);
        when(blogCommentRepo.countByBlogId(100L)).thenReturn(2L);
        when(blogLikeRepo.existsByBlogIdAndUserId(100L, 1L)).thenReturn(true);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getAllBlogs(principal("john"), 0, 10, subject);

        assertTrue(booleanField(response, "success"));

        Page<BlogDTO> data = field(response, "data");
        assertEquals(1, data.getContent().size());
        assertEquals("Title", data.getContent().get(0).getTitle());

        verify(blogRepo).findFeedBySubject(eq(1L), eq(subject), eq(UserRole.TEACHER), any(Pageable.class));
        verify(blogRepo, never()).findFeedWithoutRecommendations(anyLong(), any(), any());
        verify(blogRepo, never()).findRecommendedFeed(anyLong(), anySet(), any(), any());
    }

    @Test
    void getAllBlogs_usesFeedWithoutRecommendations_whenNoPreferredSubjects() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Subject subject = subject();

        Blog blog = blog(101L, "Title", "Content", subject, anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(studentProfileRepo.findByUser(john)).thenReturn(null);

        when(blogRepo.findFeedWithoutRecommendations(eq(1L), eq(UserRole.TEACHER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog)));

        when(blogLikeRepo.countByBlogId(101L)).thenReturn(0L);
        when(blogCommentRepo.countByBlogId(101L)).thenReturn(0L);
        when(blogLikeRepo.existsByBlogIdAndUserId(101L, 1L)).thenReturn(false);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getAllBlogs(principal("john"), -5, 100, null);

        assertTrue(booleanField(response, "success"));

        Page<BlogDTO> data = field(response, "data");
        assertEquals(1, data.getContent().size());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepo).findFeedWithoutRecommendations(eq(1L), eq(UserRole.TEACHER), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(30, pageable.getPageSize());
    }

    @Test
    void getAllBlogs_usesRecommendedFeed_whenPreferredSubjectsExist() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Subject subject = subject();

        StudentProfile profile = new StudentProfile();
        setField(profile, "subjects", Set.of(subject));

        Blog blog = blog(102L, "Recommended", "Content", subject, anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(studentProfileRepo.findByUser(john)).thenReturn(profile);

        when(blogRepo.findRecommendedFeed(eq(1L), eq(Set.of(subject)), eq(UserRole.TEACHER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog)));

        when(blogLikeRepo.countByBlogId(102L)).thenReturn(0L);
        when(blogCommentRepo.countByBlogId(102L)).thenReturn(0L);
        when(blogLikeRepo.existsByBlogIdAndUserId(102L, 1L)).thenReturn(false);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getAllBlogs(principal("john"), 0, 10, null);

        assertTrue(booleanField(response, "success"));

        Page<BlogDTO> data = field(response, "data");
        assertEquals(1, data.getContent().size());
        assertEquals("Recommended", data.getContent().get(0).getTitle());

        verify(blogRepo).findRecommendedFeed(eq(1L), eq(Set.of(subject)), eq(UserRole.TEACHER), any(Pageable.class));
    }



    @Test
    void getBlogById_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<BlogDTO> response =
                blogController.getBlogById(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("u dont have permission", stringField(response, "message"));
    }

    @Test
    void getBlogById_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<BlogDTO> response =
                blogController.getBlogById(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("blog not found", stringField(response, "message"));
    }

    @Test
    void getBlogById_returnsBlogSuccessfully() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Title", "Content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.countByBlogId(10L)).thenReturn(3L);
        when(blogCommentRepo.countByBlogId(10L)).thenReturn(4L);
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(true);

        ApiResponseWrapper<BlogDTO> response =
                blogController.getBlogById(principal("john"), 10L);

        assertTrue(booleanField(response, "success"));

        BlogDTO data = field(response, "data");
        assertEquals(10L, longField(data, "id"));
        assertEquals("Title", data.getTitle());
        assertEquals("Content", data.getContent());
    }


    @Test
    void updateBlog_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "Title", "Content", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));
    }

    @Test
    void updateBlog_returnsError_whenBlogIdIsNull() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(null, "Title", "Content", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("blog id is required", stringField(response, "message"));
    }

    @Test
    void updateBlog_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "Title", "Content", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("blog not found", stringField(response, "message"));
    }

    @Test
    void updateBlog_returnsError_whenUserIsNotOwner() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Blog blog = blog(10L, "Old", "Old content", subject(), anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "New", "New content", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("you can update only your own blog", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void updateBlog_returnsError_whenTitleIsBlank() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Old", "Old content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "   ", "New content", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("title is required", stringField(response, "message"));
    }

    @Test
    void updateBlog_returnsError_whenContentIsBlank() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Old", "Old content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "New title", "   ", subject(), Instant.now())
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("content is required", stringField(response, "message"));
    }

    @Test
    void updateBlog_returnsError_whenConflictDetected() {
        User john = user(1L, "john");

        Blog blog = blog(10L, "Old", "Old content", subject(), john);
        setField(blog, "updatedAt", Instant.parse("2026-04-13T12:00:00Z"));

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "New title", "New content", subject(), Instant.parse("2026-04-13T10:00:00Z"))
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("conflict: blog was updated on server", stringField(response, "message"));

        verify(blogRepo, never()).save(any());
    }

    @Test
    void updateBlog_returnsError_whenSubjectIsNull() {
        User john = user(1L, "john");

        Blog blog = blog(10L, "Old", "Old content", subject(), john);
        setField(blog, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "New title", "New content", null, Instant.parse("2026-04-13T12:00:00Z"))
        );

        assertFalse(booleanField(response, "success"));
        assertEquals("subject is required", stringField(response, "message"));
    }

    @Test
    void updateBlog_updatesBlogSuccessfully() {
        User john = user(1L, "john");
        Subject newSubject = subject();

        Blog blog = blog(10L, "Old", "Old content", newSubject, john);
        setField(blog, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.countByBlogId(10L)).thenReturn(1L);
        when(blogCommentRepo.countByBlogId(10L)).thenReturn(2L);
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<BlogDTO> response = blogController.updateBlog(
                principal("john"),
                blogDto(10L, "  New title  ", "  New content  ", newSubject, Instant.parse("2026-04-13T12:00:00Z"))
        );

        assertTrue(booleanField(response, "success"));

        assertEquals("New title", blog.getTitle());
        assertEquals("New content", blog.getContent());
        assertEquals(newSubject, blog.getSubject());
        assertNotNull(blog.getUpdatedAt());

        verify(blogRepo).save(blog);

        BlogDTO data = field(response, "data");
        assertEquals(10L, longField(data, "id"));
        assertEquals("New title", data.getTitle());
        assertEquals("New content", data.getContent());
    }



    @Test
    void deleteBlog_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.deleteBlog(principal("john"), blogDto(10L, "Title", "Content", subject(), null));

        assertFalse(booleanField(response, "success"));
        assertEquals("user not found", stringField(response, "message"));
    }

    @Test
    void deleteBlog_returnsError_whenIdIsNull() {
        User john = user(1L, "john");
        when(userRepo.findByUsername("john")).thenReturn(john);

        ApiResponseWrapper<String> response =
                blogController.deleteBlog(principal("john"), blogDto(null, "Title", "Content", subject(), null));

        assertFalse(booleanField(response, "success"));
        assertEquals("blog id is required", stringField(response, "message"));
    }

    @Test
    void deleteBlog_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.deleteBlog(principal("john"), blogDto(10L, "Title", "Content", subject(), null));

        assertFalse(booleanField(response, "success"));
        assertEquals("blog not found", stringField(response, "message"));
    }

    @Test
    void deleteBlog_returnsError_whenUserIsNotOwner() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Blog blog = blog(10L, "Title", "Content", subject(), anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<String> response =
                blogController.deleteBlog(principal("john"), blogDto(10L, "Title", "Content", subject(), null));

        assertFalse(booleanField(response, "success"));
        assertEquals("you can delete only your own blog", stringField(response, "message"));

        verify(blogRepo, never()).delete(any());
    }

    @Test
    void deleteBlog_deletesBlogSuccessfully() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Title", "Content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));

        ApiResponseWrapper<String> response =
                blogController.deleteBlog(principal("john"), blogDto(10L, "Title", "Content", subject(), null));

        assertTrue(booleanField(response, "success"));
        assertEquals("blog deleted", stringField(response, "data"));

        verify(blogRepo).delete(blog);
    }



    @Test
    void likeBlog_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.likeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("such user doesen't exist", stringField(response, "message"));
    }

    @Test
    void likeBlog_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.likeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("cannot find blog", stringField(response, "message"));
    }

    @Test
    void likeBlog_returnsOk_whenBlogAlreadyLiked() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Title", "Content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(true);

        ApiResponseWrapper<String> response =
                blogController.likeBlog(principal("john"), 10L);

        assertTrue(booleanField(response, "success"));
        assertEquals("blog is liked", stringField(response, "data"));

        verify(blogLikeRepo, never()).save(any());
    }

    @Test
    void likeBlog_returnsError_whenUsersAreBlocked() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Blog blog = blog(10L, "Title", "Content", subject(), anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(true);

        ApiResponseWrapper<String> response =
                blogController.likeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("cannot like post of blocked user", stringField(response, "message"));

        verify(blogLikeRepo, never()).save(any());
    }

    @Test
    void likeBlog_savesLikeSuccessfully() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Blog blog = blog(10L, "Title", "Content", subject(), anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(anna, john)).thenReturn(false);

        ApiResponseWrapper<String> response =
                blogController.likeBlog(principal("john"), 10L);

        assertTrue(booleanField(response, "success"));
        assertEquals("liked", stringField(response, "data"));

        ArgumentCaptor<BlogLike> captor = ArgumentCaptor.forClass(BlogLike.class);
        verify(blogLikeRepo).save(captor.capture());

        BlogLike like = captor.getValue();
        assertEquals(blog, like.getBlog());
        assertEquals(john, like.getUser());
    }


    @Test
    void dislikeBlog_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.dislikeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("such user doesen't exist", stringField(response, "message"));
    }

    @Test
    void dislikeBlog_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.dislikeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("cannot find blog", stringField(response, "message"));
    }

    @Test
    void dislikeBlog_returnsError_whenBlogIsNotLiked() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Title", "Content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<String> response =
                blogController.dislikeBlog(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("blog isn't liked", stringField(response, "message"));

        verify(blogLikeRepo, never()).delete(any());
    }

    @Test
    void dislikeBlog_deletesLikeSuccessfully() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "Title", "Content", subject(), john);
        BlogLike like = new BlogLike(blog, john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(true);
        when(blogLikeRepo.findByBlogIdAndUserId(10L, 1L)).thenReturn(like);

        ApiResponseWrapper<String> response =
                blogController.dislikeBlog(principal("john"), 10L);

        assertTrue(booleanField(response, "success"));
        assertEquals("deleted Succesfully", stringField(response, "data"));

        verify(blogLikeRepo).delete(like);
    }



    @Test
    void addComment_returnsError_whenContentIsBlank() {
        ApiResponseWrapper<String> response =
                blogController.addComment(principal("john"), 10L, "   ", null);

        assertFalse(booleanField(response, "success"));
        assertEquals("content can't be empty", stringField(response, "message"));

        verifyNoInteractions(userRepo);
    }

    @Test
    void addComment_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.addComment(principal("john"), 10L, "Nice", null);

        assertFalse(booleanField(response, "success"));
        assertEquals("user doesen't exists", stringField(response, "message"));
    }

    @Test
    void addComment_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.addComment(principal("john"), 10L, "Nice", null);

        assertFalse(booleanField(response, "success"));
        assertEquals("blog doesen't exist", stringField(response, "message"));
    }

    @Test
    void addComment_returnsError_whenUsersAreBlocked() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Blog blog = blog(10L, "Title", "Content", subject(), anna);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(true);

        ApiResponseWrapper<String> response =
                blogController.addComment(principal("john"), 10L, "Nice", null);

        assertFalse(booleanField(response, "success"));
        assertEquals("cannot comment post of blocked user", stringField(response, "message"));

        verify(blogCommentRepo, never()).save(any());
    }

    @Test
    void addComment_savesCommentSuccessfully() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Blog blog = blog(10L, "Title", "Content", subject(), anna);
        Instant createdAt = Instant.parse("2026-04-13T12:00:00Z");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(userBlockRepo.existsByBlockedAndBlocker(john, anna)).thenReturn(false);
        when(userBlockRepo.existsByBlockedAndBlocker(anna, john)).thenReturn(false);

        ApiResponseWrapper<String> response =
                blogController.addComment(principal("john"), 10L, "Nice comment", createdAt);

        assertTrue(booleanField(response, "success"));
        assertEquals("comment added", stringField(response, "data"));

        ArgumentCaptor<BlogComment> captor = ArgumentCaptor.forClass(BlogComment.class);
        verify(blogCommentRepo).save(captor.capture());

        BlogComment saved = captor.getValue();
        assertEquals(blog, saved.getBlog());
        assertEquals(john, saved.getAuthor());
        assertEquals("Nice comment", saved.getContent());
        assertEquals(createdAt, saved.getCreatedAt());
    }



    @Test
    void getComments_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<List<CommentDTO>> response =
                blogController.getComments(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("user doesen't exists", stringField(response, "message"));
    }

    @Test
    void getComments_returnsError_whenBlogNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.empty());

        ApiResponseWrapper<List<CommentDTO>> response =
                blogController.getComments(principal("john"), 10L);

        assertFalse(booleanField(response, "success"));
        assertEquals("blog doesen't exist", stringField(response, "message"));
    }

    @Test
    void getComments_returnsCommentsSuccessfully() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");
        Blog blog = blog(10L, "Title", "Content", subject(), john);

        BlogComment comment = comment(100L, blog, anna, "Hello", Instant.parse("2026-04-13T12:00:00Z"));

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findById(10L)).thenReturn(Optional.of(blog));
        when(blogCommentRepo.findByBlogId(10L)).thenReturn(List.of(comment));

        ApiResponseWrapper<List<CommentDTO>> response =
                blogController.getComments(principal("john"), 10L);

        assertTrue(booleanField(response, "success"));

        List<CommentDTO> data = field(response, "data");
        assertEquals(1, data.size());
        assertEquals(100L, longField(data.get(0), "id"));
        assertEquals("Hello", stringField(data.get(0), "content"));
    }



    @Test
    void deleteComment_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.deleteComment(principal("john"), 100L);

        assertFalse(booleanField(response, "success"));
        assertEquals("user doesen't exists", stringField(response, "message"));
    }

    @Test
    void deleteComment_returnsError_whenCommentNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.deleteComment(principal("john"), 100L);

        assertFalse(booleanField(response, "success"));
        assertEquals("coment doesent exist", stringField(response, "message"));
    }

    @Test
    void deleteComment_allowsBlogAuthorToDeleteComment() {
        User blogAuthor = user(1L, "john");
        User commentAuthor = user(2L, "anna");

        Blog blog = blog(10L, "Title", "Content", subject(), blogAuthor);
        BlogComment comment = comment(100L, blog, commentAuthor, "Hello", Instant.now());

        when(userRepo.findByUsername("john")).thenReturn(blogAuthor);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.deleteComment(principal("john"), 100L);

        assertTrue(booleanField(response, "success"));
        assertEquals("deleted succesfully", stringField(response, "data"));

        verify(blogCommentRepo).delete(comment);
    }

    @Test
    void deleteComment_returnsError_whenUserIsNotBlogAuthorAndNotCommentAuthor() {
        User john = user(1L, "john");
        User blogAuthor = user(2L, "anna");
        User commentAuthor = user(3L, "maria");

        Blog blog = blog(10L, "Title", "Content", subject(), blogAuthor);
        BlogComment comment = comment(100L, blog, commentAuthor, "Hello", Instant.now());

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.deleteComment(principal("john"), 100L);

        assertFalse(booleanField(response, "success"));
        assertEquals(
                "u ccannot remove comment that doesent belong to u if u are not the creator of the blog",
                stringField(response, "message")
        );

        verify(blogCommentRepo, never()).delete(any());
    }

    @Test
    void deleteComment_allowsCommentAuthorToDeleteOwnComment() {
        User blogAuthor = user(1L, "john");
        User commentAuthor = user(2L, "anna");

        Blog blog = blog(10L, "Title", "Content", subject(), blogAuthor);
        BlogComment comment = comment(100L, blog, commentAuthor, "Hello", Instant.now());

        when(userRepo.findByUsername("anna")).thenReturn(commentAuthor);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.deleteComment(principal("anna"), 100L);

        assertTrue(booleanField(response, "success"));
        assertEquals("deleted succcesfully", stringField(response, "data"));

        verify(blogCommentRepo).delete(comment);
    }



    @Test
    void editComment_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<String> response =
                blogController.editComment(principal("john"), 100L, "Updated");

        assertFalse(booleanField(response, "success"));
        assertEquals("user doesen't exists", stringField(response, "message"));
    }

    @Test
    void editComment_returnsError_whenCommentNotFound() {
        User john = user(1L, "john");

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.empty());

        ApiResponseWrapper<String> response =
                blogController.editComment(principal("john"), 100L, "Updated");

        assertFalse(booleanField(response, "success"));
        assertEquals("coment doesent exist", stringField(response, "message"));
    }

    @Test
    void editComment_returnsError_whenUserIsNotAuthor() {
        User john = user(1L, "john");
        User anna = user(2L, "anna");

        Blog blog = blog(10L, "Title", "Content", subject(), john);
        BlogComment comment = comment(100L, blog, anna, "Hello", Instant.now());

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.editComment(principal("john"), 100L, "Updated");

        assertFalse(booleanField(response, "success"));
        assertEquals(
                "u ccannot edit comment that doesent belong to u if u are not the creator of the blog",
                stringField(response, "message")
        );

        verify(blogCommentRepo, never()).save(any());
    }

    @Test
    void editComment_returnsError_whenContentIsBlank() {
        User john = user(1L, "john");

        Blog blog = blog(10L, "Title", "Content", subject(), john);
        BlogComment comment = comment(100L, blog, john, "Hello", Instant.now());

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.editComment(principal("john"), 100L, "   ");

        assertFalse(booleanField(response, "success"));
        assertEquals("content can't be empty", stringField(response, "message"));

        verify(blogCommentRepo, never()).save(any());
    }

    @Test
    void editComment_updatesCommentSuccessfully() {
        User john = user(1L, "john");

        Blog blog = blog(10L, "Title", "Content", subject(), john);
        BlogComment comment = comment(100L, blog, john, "Old", Instant.now());

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogCommentRepo.findById(100L)).thenReturn(Optional.of(comment));

        ApiResponseWrapper<String> response =
                blogController.editComment(principal("john"), 100L, "Updated");

        assertTrue(booleanField(response, "success"));
        assertEquals("updated succesfully", stringField(response, "data"));
        assertEquals("Updated", comment.getContent());

        verify(blogCommentRepo).save(comment);
    }



    @Test
    void getMyBlogs_returnsError_whenUserNotFound() {
        when(userRepo.findByUsername("john")).thenReturn(null);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getMyBlogs(principal("john"), 0, 10);

        assertFalse(booleanField(response, "success"));
        assertEquals("u dont have permission", stringField(response, "message"));
    }

    @Test
    void getMyBlogs_returnsMyBlogsSuccessfully() {
        User john = user(1L, "john");
        Blog blog = blog(10L, "My blog", "Content", subject(), john);

        when(userRepo.findByUsername("john")).thenReturn(john);
        when(blogRepo.findByAuthorIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog)));

        when(blogLikeRepo.countByBlogId(10L)).thenReturn(2L);
        when(blogCommentRepo.countByBlogId(10L)).thenReturn(3L);
        when(blogLikeRepo.existsByBlogIdAndUserId(10L, 1L)).thenReturn(false);

        ApiResponseWrapper<Page<BlogDTO>> response =
                blogController.getMyBlogs(principal("john"), -1, 100);

        assertTrue(booleanField(response, "success"));

        Page<BlogDTO> data = field(response, "data");
        assertEquals(1, data.getContent().size());
        assertEquals("My blog", data.getContent().get(0).getTitle());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepo).findByAuthorIdOrderByCreatedAtDesc(eq(1L), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(30, pageable.getPageSize());
    }



    private Subject subject() {
        return Subject.values()[0];
    }

    private BlogDTO blogDto(Long id, String title, String content, Subject subject, Instant updatedAt) {
        return new BlogDTO(
                id,
                title,
                content,
                subject,
                0L,
                "author",
                null,
                updatedAt,
                0L,
                0L,
                false
        );
    }

    private User user(Long id, String username) {
        User user = new User();
        setField(user, "id", id);
        setField(user, "username", username);
        return user;
    }

    private Blog blog(Long id, String title, String content, Subject subject, User author) {
        Blog blog = new Blog(title, content, subject, author);
        setField(blog, "id", id);
        setField(blog, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        setField(blog, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
        return blog;
    }

    private BlogComment comment(Long id, Blog blog, User author, String content, Instant createdAt) {
        BlogComment comment = new BlogComment(blog, author, content, createdAt);
        setField(comment, "id", id);
        return comment;
    }

    private boolean booleanField(Object target, String fieldName) {
        Object value = field(target, fieldName);
        return Boolean.TRUE.equals(value);
    }

    private String stringField(Object target, String fieldName) {
        Object value = field(target, fieldName);
        return value == null ? null : value.toString();
    }

    private long longField(Object target, String fieldName) {
        return ((Number) field(target, fieldName)).longValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read field: " + fieldName, e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }
}