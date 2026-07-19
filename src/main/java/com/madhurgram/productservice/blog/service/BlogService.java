package com.madhurgram.productservice.blog.service;

import com.madhurgram.productservice.blog.dto.BlogDTO;
import java.util.List;

public interface BlogService {
    List<BlogDTO> getAllActiveBlogs();
    BlogDTO getBlogBySlug(String slug);
}
