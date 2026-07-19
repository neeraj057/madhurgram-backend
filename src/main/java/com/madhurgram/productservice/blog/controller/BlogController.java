package com.madhurgram.productservice.blog.controller;

import com.madhurgram.productservice.blog.dto.BlogDTO;
import com.madhurgram.productservice.blog.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs", description = "Public endpoints for Village Stories & Blogs")
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "Get all active blogs")
    public ResponseEntity<List<BlogDTO>> getAllBlogs() {
        return ResponseEntity.ok(blogService.getAllActiveBlogs());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a specific blog by slug")
    public ResponseEntity<BlogDTO> getBlogBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getBlogBySlug(slug));
    }
}
