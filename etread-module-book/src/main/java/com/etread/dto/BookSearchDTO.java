package com.etread.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class BookSearchDTO {
    private Long id;
    private String title;
    private String author;
    private Long publisher;
    private List<String> tags;
    //最少字数
    private Integer minwordcount;
    //最大字数
    private Integer maxwordcount;
    //最低评分
    private Double minscore;
    //最高评分
    private Double maxscore;
    //创造时间
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createtime;
    //更新时间
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatetime;
    private Integer tagCount;
}
