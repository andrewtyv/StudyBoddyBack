package controllers;

import DTO.*;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import repos.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/blog")
@Tag(name = "Blog", description = "Endpoints for creating, reading, updating, and deleting blog posts")
public class BlogController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    BlogRepo blogRepo;
    @Operation(
            summary = "Create a blog post",
            description = "Creates a new blog post for the currently authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blog post created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "message": "ok",
                                              "data": {
                                                "id": 1,
                                                "title": "My first blog",
                                                "content": "This is my blog content",
                                                "authorId": 5,
                                                "authorUsername": "nazar",
                                                "createdAt": "2026-03-31T12:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or user error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "title is required",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "content is required",
                                              "data": null
                                            }
                                            """)
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

        if (title == null || title.trim().isBlank()) {
            return ApiResponseWrapper.error("title is required");
        }

        if (content == null || content.trim().isBlank()) {
            return ApiResponseWrapper.error("content is required");
        }

        Blog blog = new Blog(title.trim(), content.trim(), me);
        blogRepo.save(blog);

        return ApiResponseWrapper.ok(
                new BlogDTO(
                        blog.getId(),
                        blog.getTitle(),
                        blog.getContent(),
                        blog.getAuthor().getId(),
                        blog.getAuthor().getUsername(),
                        blog.getCreatedAt()
                )
        );
    }
    @Operation(
            summary = "Get all blog posts",
            description = "Returns all blog posts. Teacher posts are shown first, then the rest ordered by creation date descending."
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
                                              "message": "ok",
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "title": "Teacher announcement",
                                                  "content": "Important update",
                                                  "authorId": 2,
                                                  "authorUsername": "teacher1",
                                                  "createdAt": "2026-03-31T10:00:00"
                                                },
                                                {
                                                  "id": 2,
                                                  "title": "Student post",
                                                  "content": "Hello everyone",
                                                  "authorId": 5,
                                                  "authorUsername": "nazar",
                                                  "createdAt": "2026-03-30T18:00:00"
                                                }
                                              ]
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

            if (aTeacher && !bTeacher) {
                return -1;
            }
            if (!aTeacher && bTeacher) {
                return 1;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<BlogDTO> dto = new ArrayList<>();

        for (Blog blog : blogs) {
            dto.add(new BlogDTO(
                    blog.getId(),
                    blog.getTitle(),
                    blog.getContent(),
                    blog.getAuthor().getId(),
                    blog.getAuthor().getUsername(),
                    blog.getCreatedAt()
            ));
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
                                              "message": "ok",
                                              "data": {
                                                "id": 1,
                                                "title": "My first blog",
                                                "content": "This is my blog content",
                                                "authorId": 5,
                                                "authorUsername": "nazar",
                                                "createdAt": "2026-03-31T12:30:00"
                                              }
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
                                              "data": null
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

        Blog blog = blogOptional.get();

        return ApiResponseWrapper.ok(
                new BlogDTO(
                        blog.getId(),
                        blog.getTitle(),
                        blog.getContent(),
                        blog.getAuthor().getId(),
                        blog.getAuthor().getUsername(),
                        blog.getCreatedAt()
                )
        );
    }
    @Operation(
            summary = "Update a blog post",
            description = "Updates an existing blog post. Only the author of the post can update it."
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
                                              "message": "ok",
                                              "data": {
                                                "id": 1,
                                                "title": "Updated title",
                                                "content": "Updated content",
                                                "authorId": 5,
                                                "authorUsername": "nazar",
                                                "createdAt": "2026-03-31T12:30:00"
                                              }
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
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "blog id is required",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "you can update only your own blog",
                                              "data": null
                                            }
                                            """)
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

        blog.setTitle(title.trim());
        blog.setContent(content.trim());
        blogRepo.save(blog);

        return ApiResponseWrapper.ok(
                new BlogDTO(
                        blog.getId(),
                        blog.getTitle(),
                        blog.getContent(),
                        blog.getAuthor().getId(),
                        blog.getAuthor().getUsername(),
                        blog.getCreatedAt()
                )
        );
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
                                              "message": "ok",
                                              "data": "blog deleted"
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
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "blog id is required",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(value = """
                                            {
                                              "success": false,
                                              "message": "you can delete only your own blog",
                                              "data": null
                                            }
                                            """)
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