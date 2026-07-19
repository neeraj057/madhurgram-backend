package com.madhurgram.productservice.blog.service.impl;

import com.madhurgram.productservice.blog.dto.BlogDTO;
import com.madhurgram.productservice.blog.entity.Blog;
import com.madhurgram.productservice.blog.repository.BlogRepository;
import com.madhurgram.productservice.blog.service.BlogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    @Override
    public List<BlogDTO> getAllActiveBlogs() {
        return blogRepository.findByIsActiveTrueOrderByPublishedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BlogDTO getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        return mapToDTO(blog);
    }

    private BlogDTO mapToDTO(Blog blog) {
        return BlogDTO.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .author(blog.getAuthor())
                .content(blog.getContent())
                .imageUrl(blog.getImageUrl())
                .category(blog.getCategory())
                .publishedAt(blog.getPublishedAt())
                .build();
    }

    @PostConstruct
    public void initDummyData() {
        if (blogRepository.count() == 0) {
            log.info("Inserting dummy blog data for UI testing...");
            Blog b1 = Blog.builder()
                    .title("Bilona Ghee Kaise Banta Hai?")
                    .slug("bilona-ghee-making-process")
                    .author("MadhurGram Farmers")
                    .category("Village Process")
                    .imageUrl("/images/ghee-making.jpg")
                    .content("<p>Humara A2 Bilona Ghee traditional tarike se banta hai. Dudh ko dahi banakar fir bilone se matha jata hai, jisse makkhan nikalta hai. Is prakriya mein kisi bhi prakar ka chemical ya machine ka prayog nahi hota.</p><br/><h3>Fayde:</h3><ul><li>Pachan ke liye halka</li><li>Poshan se bharpur</li></ul>")
                    .isActive(true)
                    .build();

            Blog b2 = Blog.builder()
                    .title("Gud (Jaggery) Khaane ke 5 Fayde")
                    .slug("health-benefits-of-jaggery")
                    .author("MadhurGram Experts")
                    .category("Health Benefits")
                    .imageUrl("/images/jaggery.jpg")
                    .content("<p>Gud ek prakritik meetha hai jo chini (sugar) ka ek healthy vikalp hai. Isme iron aur minerals bharpur matra mein hote hain.</p><br/><h3>Kyu khayein?</h3><ul><li>Khoob ki kami dur karta hai</li><li>Immunity badhata hai</li><li>Pachan thik rakhta hai</li></ul>")
                    .isActive(true)
                    .build();

            blogRepository.saveAll(List.of(b1, b2));
        }
    }
}
