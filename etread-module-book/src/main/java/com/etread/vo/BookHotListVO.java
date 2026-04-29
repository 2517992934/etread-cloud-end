package com.etread.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookHotListVO {
    private Integer rank;
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer totalScore;
    private Integer ratingCount;
    private Double averageScore;
    private Double hotScore;
}
