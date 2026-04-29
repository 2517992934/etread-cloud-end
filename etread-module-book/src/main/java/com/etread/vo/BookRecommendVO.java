package com.etread.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRecommendVO {
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private String description;
    private Integer wordCount;
    private Integer totalScore;
    private Integer ratingCount;
    private Double averageScore;
}
