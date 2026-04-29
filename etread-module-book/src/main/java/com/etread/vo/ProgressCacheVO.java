package com.etread.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressCacheVO implements Serializable {
    private Long bookId;
    private Long currentChapterId;
    private Float readPercentage;
    private Date lastReadTime;
    private Boolean dirty;
}
