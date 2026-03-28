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

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    BlogRepo blogRepo;

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