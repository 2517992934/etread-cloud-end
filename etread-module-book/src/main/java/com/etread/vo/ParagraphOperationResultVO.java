package com.etread.vo;

import lombok.Data;

@Data
public class ParagraphOperationResultVO {

    private Long chapterId;

    private String content;

    private Integer chapterWordCount;

    private Integer bookWordCount;
}
