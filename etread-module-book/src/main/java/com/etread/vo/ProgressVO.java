package com.etread.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressVO {
    private Long userId;
    private Long bookId;
    private Long currentChapterId;
    private Float readPercentage;
    private Date lastReadTime;
}
