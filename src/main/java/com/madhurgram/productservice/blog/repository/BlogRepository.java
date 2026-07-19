package com.madhurgram.productservice.blog.repository;

import com.madhurgram.productservice.blog.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBySlugAndIsActiveTrue(String slug);
    List<Blog> findByIsActiveTrueOrderByPublishedAtDesc();
}
