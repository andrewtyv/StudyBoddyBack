package controllers;

import DTO.ApiResponseWrapper;
import DTO.BlogDTO;
import DTO.*;
import model.*;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import repos.*;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/blog")
@Tag(name = "Blog", description = "Endpoints for creating, reading, updating, and deleting blog posts")
public class BlogController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    BlogRepo blogRepo;

    @Autowired
    BlogLikeRepo blogLikeRepo;

    @Autowired
    BlogCommentRepo blogCommentRepo;

    @Autowired
    UserBlockRepo userBlockRepo;

    @Autowired
    StudentProfileRepo studentProfileRepo;

    private BlogDTO toDto(Blog blog, Long likes, Long comments, Boolean likedByMe) {
        return new BlogDTO(
                blog.getId(),
                blog.getTitle(),
                blog.getContent(),
                blog.getSubject(),
                blog.getAuthor().getId(),
                blog.getAuthor().getUsername(),
                blog.getCreatedAt(),
                blog.getUpdatedAt(),
                likes,
                comments,
                likedByMe
        );
    }

    @Operation(
            summary = "Create a blog post",
            description = "Creates a new blog post for the currently authenticated user. If clientId is provided and the same post was already synchronized before, the existing blog post is returned instead of creating a duplicate."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blog post created successfully or existing synchronized blog returned",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "created",
                                            value = """
                                                {
                                                  "success": true,
                                                  "message": null,
                                                  "data": {
                                                    "id": 1,
                                                    "title": "My first blog",
                                                    "content": "This is my blog content",
                                                    "authorId": 5,
                                                    "authorUsername": "nazar",
                                                    "createdAt": "2026-04-13T12:30:00Z",
                                                    "updatedAt": "2026-04-13T12:30:00Z",
                                                    "clientId": "550e8400-e29b-41d4-a716-446655440000"
                                                  },
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "already_synced_same_client_id",
                                            value = """
                                                {
                                                  "success": true,
                                                  "message": null,
                                                  "data": {
                                                    "id": 1,
                                                    "title": "My first blog",
                                                    "content": "This is my blog content",
                                                    "authorId": 5,
                                                    "authorUsername": "nazar",
                                                    "createdAt": "2026-04-13T12:30:00Z",
                                                    "updatedAt": "2026-04-13T12:30:00Z",
                                                    "clientId": "550e8400-e29b-41d4-a716-446655440000"
                                                  },
                                                  "token": null
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or user error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "user_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "user not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "title_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "title is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "content_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "content is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/create")
    public ApiResponseWrapper<BlogDTO> createBlog(Principal principal, @RequestBody BlogDTO body) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        String title = body.getTitle();
        String content = body.getContent();
        Subject subject = body.getSubject();

        if (title == null || title.trim().isBlank()) {
            return ApiResponseWrapper.error("title is required");
        }

        if (content == null || content.trim().isBlank()) {
            return ApiResponseWrapper.error("content is required");
        }

        if (subject == null) {
            return ApiResponseWrapper.error("subject is required");
        }

        Blog blog = new Blog(title.trim(), content.trim(), subject, me);

        blogRepo.save(blog);

        return ApiResponseWrapper.ok(toDto(
                blog,
                blogLikeRepo.countByBlogId(blog.getId()),
                blogCommentRepo.countByBlogId(blog.getId()),
                blogLikeRepo.existsByBlogIdAndUserId(blog.getId(), me.getId())
        ));
    }

    @Operation(
            summary = "Get all blog posts",
            description = "Returns all blog posts. Teacher posts are shown first, then the remaining posts are ordered by creation date descending."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of blog posts returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": null,
                                          "data": [
                                            {
                                              "id": 1,
                                              "title": "Teacher announcement",
                                              "content": "Important update",
                                              "authorId": 2,
                                              "authorUsername": "teacher1",
                                              "createdAt": "2026-04-13T10:00:00Z",
                                              "updatedAt": "2026-04-13T10:00:00Z",
                                              "clientId": "teacher-post-1"
                                            },
                                            {
                                              "id": 2,
                                              "title": "Student post",
                                              "content": "Hello everyone",
                                              "authorId": 5,
                                              "authorUsername": "nazar",
                                              "createdAt": "2026-04-12T18:00:00Z",
                                              "updatedAt": "2026-04-12T18:10:00Z",
                                              "clientId": "550e8400-e29b-41d4-a716-446655440000"
                                            }
                                          ],
                                          "token": null
                                        }
                                        """
                            )
                    )
            )
    })
    @GetMapping("/all")
    public ApiResponseWrapper<Page<BlogDTO>> getAllBlogs(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Subject subject
    ) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("u dont have permission");
        }

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 30) {
            size = 30;
        }

        Pageable pageable = PageRequest.of(page, size);

        StudentProfile studentProfile = studentProfileRepo.findByUser(me);

        Set<Subject> preferredSubjects = Set.of();

        if (studentProfile != null && studentProfile.getSubjects() != null) {
            preferredSubjects = studentProfile.getSubjects();
        }

        Page<Blog> blogsPage;

        if (subject != null) {
            blogsPage = blogRepo.findFeedBySubject(
                    me.getId(),
                    subject,
                    UserRole.TEACHER,
                    pageable
            );
        } else if (preferredSubjects.isEmpty()) {
            blogsPage = blogRepo.findFeedWithoutRecommendations(
                    me.getId(),
                    UserRole.TEACHER,
                    pageable
            );
        } else {
            blogsPage = blogRepo.findRecommendedFeed(
                    me.getId(),
                    preferredSubjects,
                    UserRole.TEACHER,
                    pageable
            );
        }

        Page<BlogDTO> dtoPage = blogsPage.map(blog -> {
            Boolean likedByMe = blogLikeRepo.existsByBlogIdAndUserId(
                    blog.getId(),
                    me.getId()
            );

            return toDto(
                    blog,
                    blogLikeRepo.countByBlogId(blog.getId()),
                    blogCommentRepo.countByBlogId(blog.getId()),
                    likedByMe
            );
        });

        return ApiResponseWrapper.ok(dtoPage);
    }
    @Operation(
            summary = "Get blog post by ID",
            description = "Returns one blog post by its ID."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blog post found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": null,
                                          "data": {
                                            "id": 1,
                                            "title": "My first blog",
                                            "content": "This is my blog content",
                                            "authorId": 5,
                                            "authorUsername": "nazar",
                                            "createdAt": "2026-04-13T12:30:00Z",
                                            "updatedAt": "2026-04-13T13:00:00Z",
                                            "clientId": "550e8400-e29b-41d4-a716-446655440000"
                                          },
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Blog post not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": false,
                                          "message": "blog not found",
                                          "data": null,
                                          "token": null
                                        }
                                        """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ApiResponseWrapper<BlogDTO> getBlogById(Principal principal,@PathVariable Long id) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null){
            return ApiResponseWrapper.error("u dont have permission");
        }
        Optional<Blog> blogOptional = blogRepo.findById(id);

        if (blogOptional.isEmpty()) {
            return ApiResponseWrapper.error("blog not found");
        }

        return ApiResponseWrapper.ok(toDto(blogOptional.get(), blogLikeRepo.countByBlogId(blogOptional.get().getId()), blogCommentRepo.countByBlogId(blogOptional.get().getId()),blogLikeRepo.existsByBlogIdAndUserId(blogOptional.get().getId(),me.getId())));
    }

    @Operation(
            summary = "Update a blog post",
            description = "Updates an existing blog post. Only the author of the post can update it. If the server version is newer than the client's known version, a conflict message is returned."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blog post updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": null,
                                          "data": {
                                            "id": 1,
                                            "title": "Updated title",
                                            "content": "Updated content",
                                            "authorId": 5,
                                            "authorUsername": "nazar",
                                            "createdAt": "2026-04-13T12:30:00Z",
                                            "updatedAt": "2026-04-13T14:20:00Z",
                                            "clientId": "550e8400-e29b-41d4-a716-446655440000"
                                          },
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation, ownership, or synchronization conflict error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "user_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "user not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "blog_id_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "blog id is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "blog_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "blog not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "not_owner",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "you can update only your own blog",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "title_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "title is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "content_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "content is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "conflict",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "conflict: blog was updated on server",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    @PutMapping("/update")
    public ApiResponseWrapper<BlogDTO> updateBlog(
            Principal principal,
            @RequestBody BlogDTO body
    ) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (body.getId() == null) {
            return ApiResponseWrapper.error("blog id is required");
        }

        Optional<Blog> blogOptional = blogRepo.findById(body.getId());
        if (blogOptional.isEmpty()) {
            return ApiResponseWrapper.error("blog not found");
        }

        Blog blog = blogOptional.get();

        if (!blog.getAuthor().getId().equals(me.getId())) {
            return ApiResponseWrapper.error("you can update only your own blog");
        }

        String title = body.getTitle();
        String content = body.getContent();

        if (title == null || title.trim().isBlank()) {
            return ApiResponseWrapper.error("title is required");
        }

        if (content == null || content.trim().isBlank()) {
            return ApiResponseWrapper.error("content is required");
        }

        if (body.getUpdatedAt() != null && blog.getUpdatedAt() != null) {
            if (blog.getUpdatedAt().isAfter(body.getUpdatedAt())) {
                return ApiResponseWrapper.error("conflict: blog was updated on server");
            }
        }
        Subject subject = body.getSubject();

        if (subject == null) {
            return ApiResponseWrapper.error("subject is required");
        }

        blog.setTitle(title.trim());
        blog.setContent(content.trim());
        blog.setUpdatedAt(Instant.now());
        blog.setSubject(subject);

        blogRepo.save(blog);

        return ApiResponseWrapper.ok(toDto(blog,blogLikeRepo.countByBlogId(blog.getId()), blogCommentRepo.countByBlogId(blog.getId()),blogLikeRepo.existsByBlogIdAndUserId(blog.getId(),me.getId())));
    }

    @Operation(
            summary = "Delete a blog post",
            description = "Deletes a blog post. Only the author of the post can delete it."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blog post deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "message": null,
                                          "data": "blog deleted",
                                          "token": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or ownership error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "user_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "user not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "blog_id_required",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "blog id is required",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "blog_not_found",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "blog not found",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "not_owner",
                                            value = """
                                                {
                                                  "success": false,
                                                  "message": "you can delete only your own blog",
                                                  "data": null,
                                                  "token": null
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    @DeleteMapping("/delete")
    public ApiResponseWrapper<String> deleteBlog(Principal principal, @RequestBody BlogDTO body) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user not found");
        }

        if (body.getId() == null) {
            return ApiResponseWrapper.error("blog id is required");
        }

        Optional<Blog> blogOptional = blogRepo.findById(body.getId());
        if (blogOptional.isEmpty()) {
            return ApiResponseWrapper.error("blog not found");
        }

        Blog blog = blogOptional.get();

        if (!blog.getAuthor().getId().equals(me.getId())) {
            return ApiResponseWrapper.error("you can delete only your own blog");
        }

        blogRepo.delete(blog);
        return ApiResponseWrapper.ok("blog deleted");
    }

    @PostMapping("/like/{id}")
    public ApiResponseWrapper<String> likeBlog(Principal principal, @PathVariable Long id) {
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("such user doesen't exist");
        }
        Blog blog = blogRepo.findById(id).orElse(null);
        if (blog == null) {
            return ApiResponseWrapper.error("cannot find blog");
        }
        if (blogLikeRepo.existsByBlogIdAndUserId(blog.getId(), me.getId())) {
            return ApiResponseWrapper.ok("blog is liked");
        }
        if (userBlockRepo.existsByBlockedAndBlocker(me,blog.getAuthor()) || userBlockRepo.existsByBlockedAndBlocker(blog.getAuthor(),me)) {
            return ApiResponseWrapper.error("cannot like post of blocked user");
        }
        BlogLike like = new BlogLike(blog, me);
        blogLikeRepo.save(like);
        return ApiResponseWrapper.ok("liked");
    }
    @DeleteMapping("/dislike/{id}")
    public ApiResponseWrapper<String> dislikeBlog(Principal principal, @PathVariable Long id){
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("such user doesen't exist");
        }
        Blog blog = blogRepo.findById(id).orElse(null);
        if (blog == null) {
            return ApiResponseWrapper.error("cannot find blog");
        }
        if (!blogLikeRepo.existsByBlogIdAndUserId(blog.getId(), me.getId())) {
            return ApiResponseWrapper.error("blog isn't liked");
        }
        BlogLike like = blogLikeRepo.findByBlogIdAndUserId(blog.getId(),me.getId());
        blogLikeRepo.delete(like);
        return ApiResponseWrapper.ok("deleted Succesfully");

    }

    @PostMapping("/comment/{id}")
    public ApiResponseWrapper<String> addComment(Principal principal, @PathVariable Long id, @RequestParam String content, @RequestParam(required = false) Instant createdAt){
        if (content == null || content.isEmpty() || content.isBlank()){
            return ApiResponseWrapper.error("content can't be empty");
        }
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user doesen't exists");
        }
        Blog blog = blogRepo.findById(id).orElse(null);
        if(blog == null) {
            return ApiResponseWrapper.error("blog doesen't exist");
        }
        if (userBlockRepo.existsByBlockedAndBlocker(me,blog.getAuthor()) || userBlockRepo.existsByBlockedAndBlocker(blog.getAuthor(),me)) {
            return ApiResponseWrapper.error("cannot comment post of blocked user");
        }
        Instant finalCreatedAt = createdAt != null ? createdAt : Instant.now();

        BlogComment comment = new BlogComment(blog,me ,content,finalCreatedAt);
        blogCommentRepo.save(comment);
        return ApiResponseWrapper.ok("comment added");
    }

    @GetMapping("/comments/{id}")
    public ApiResponseWrapper<List<CommentDTO>> getComments(Principal principal, @PathVariable Long id){
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user doesen't exists");
        }
        Blog blog = blogRepo.findById(id).orElse(null);
        if(blog == null) {
            return ApiResponseWrapper.error("blog doesen't exist");
        }
        List<BlogComment> comments = blogCommentRepo.findByBlogId(blog.getId());
        return ApiResponseWrapper.ok(
                comments.stream().map(fr -> new CommentDTO(fr.getId(),fr.getAuthor().getUsername(),
                        fr.getContent(), fr.getCreatedAt())).toList());
    }
    @DeleteMapping("/comments/{comment_id}")
    public ApiResponseWrapper<String> deleteComment(Principal principal, @PathVariable("comment_id") Long commentId){
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user doesen't exists");
        }
        BlogComment comment = blogCommentRepo.findById(commentId).orElse(null);
        if (comment == null) {
            return ApiResponseWrapper.error("coment doesent exist");
        }
        if(me.getId().equals(comment.getBlog().getAuthor().getId())) {
            blogCommentRepo.delete(comment);
            return ApiResponseWrapper.ok("deleted succesfully");
        }
        if(!comment.getAuthor().getId().equals( me.getId())) {
            return ApiResponseWrapper.error("u ccannot remove comment that doesent belong to u if u are not the creator of the blog");
        }
        blogCommentRepo.delete(comment);
        return ApiResponseWrapper.ok("deleted succcesfully");

    }

    @PutMapping("/comments/{comment_id}")
    public ApiResponseWrapper<String> editComment(Principal principal, @PathVariable("comment_id") Long commentId, @RequestParam String content ){
        User me = userRepo.findByUsername(principal.getName());
        if (me == null) {
            return ApiResponseWrapper.error("user doesen't exists");
        }
        BlogComment comment = blogCommentRepo.findById(commentId).orElse(null);
        if (comment == null) {
            return ApiResponseWrapper.error("coment doesent exist");
        }
        if(!comment.getAuthor().getId().equals(me.getId())) {
            return ApiResponseWrapper.error("u ccannot edit comment that doesent belong to u if u are not the creator of the blog");
        }
        if (content == null || content.trim().isBlank()) {
            return ApiResponseWrapper.error("content can't be empty");
        }
        comment.setContent(content);
        blogCommentRepo.save(comment);
        return ApiResponseWrapper.ok("updated succesfully");

    }

    @GetMapping("/my")
    public ApiResponseWrapper<Page<BlogDTO>> getMyBlogs(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("u dont have permission");
        }

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 30) {
            size = 30;
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Blog> blogsPage = blogRepo.findByAuthorIdOrderByCreatedAtDesc(
                me.getId(),
                pageable
        );

        Page<BlogDTO> dtoPage = blogsPage.map(blog -> {
            Boolean likedByMe = blogLikeRepo.existsByBlogIdAndUserId(
                    blog.getId(),
                    me.getId()
            );

            return toDto(
                    blog,
                    blogLikeRepo.countByBlogId(blog.getId()),
                    blogCommentRepo.countByBlogId(blog.getId()),
                    likedByMe
            );
        });

        return ApiResponseWrapper.ok(dtoPage);
    }



}