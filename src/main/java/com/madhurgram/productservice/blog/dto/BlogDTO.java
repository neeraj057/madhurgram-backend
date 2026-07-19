package com.madhurgram.productservice.blog.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BlogDTO {
    private Long id;
    private String title;
    private String slug;
    private String author;
    private String content;
    private String imageUrl;
    private String category;
    private LocalDateTime publishedAt;
}
