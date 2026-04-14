package com.etread.vo;

import lombok.Data;

@Data
public class BookInfoBaseVO {

    private Long id;
    private String title;
    private String author;
    private String coverUrl;
    private String description;
    private Integer wordCount;
    private Integer totalScore;
    private Integer ratingCount;
    private Double averageScore;
}
