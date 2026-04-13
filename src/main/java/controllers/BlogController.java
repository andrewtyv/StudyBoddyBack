package controllers;

import DTO.ApiResponseWrapper;
import DTO.BlogDTO;
import model.Blog;
import model.User;
import model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import repos.BlogRepo;
import repos.UserRepo;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private BlogDTO toDto(Blog blog) {
        return new BlogDTO(
                blog.getId(),
                blog.getTitle(),
                blog.getContent(),
                blog.getAuthor().getId(),
                blog.getAuthor().getUsername(),
                blog.getCreatedAt(),
                blog.getUpdatedAt(),
                blog.getClientId()
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
        String clientId = body.getClientId() != null ? body.getClientId().trim() : null;

        if (title == null || title.trim().isBlank()) {
            return ApiResponseWrapper.error("title is required");
        }

        if (content == null || content.trim().isBlank()) {
            return ApiResponseWrapper.error("content is required");
        }

        if (clientId != null && !clientId.isBlank()) {
            Optional<Blog> existing = blogRepo.findByClientIdAndAuthor_Id(clientId, me.getId());
            if (existing.isPresent()) {
                return ApiResponseWrapper.ok(toDto(existing.get()));
            }
        }

        Blog blog = new Blog(title.trim(), content.trim(), me);
        if (clientId != null && !clientId.isBlank()) {
            blog.setClientId(clientId);
        }

        blogRepo.save(blog);
        return ApiResponseWrapper.ok(toDto(blog));
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
    public ApiResponseWrapper<List<BlogDTO>> getAllBlogs(Principal principal) {
        List<Blog> blogs = blogRepo.findAllByOrderByCreatedAtDesc();

        blogs.sort((a, b) -> {
            boolean aTeacher = a.getAuthor().getRole() == UserRole.TEACHER;
            boolean bTeacher = b.getAuthor().getRole() == UserRole.TEACHER;

            if (aTeacher && !bTeacher) return -1;
            if (!aTeacher && bTeacher) return 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<BlogDTO> dto = new ArrayList<>();
        for (Blog blog : blogs) {
            dto.add(toDto(blog));
        }

        return ApiResponseWrapper.ok(dto);
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
    public ApiResponseWrapper<BlogDTO> getBlogById(@PathVariable Long id) {
        Optional<Blog> blogOptional = blogRepo.findById(id);

        if (blogOptional.isEmpty()) {
            return ApiResponseWrapper.error("blog not found");
        }

        return ApiResponseWrapper.ok(toDto(blogOptional.get()));
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

        blog.setTitle(title.trim());
        blog.setContent(content.trim());
        blog.setUpdatedAt(Instant.now());

        blogRepo.save(blog);

        return ApiResponseWrapper.ok(toDto(blog));
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
}