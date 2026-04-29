package com.etread.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private Long userId;
    private Long bookId;
    private Long currentChapterId;
    private Float readPercentage;
    private Date lastReadTime;
}
