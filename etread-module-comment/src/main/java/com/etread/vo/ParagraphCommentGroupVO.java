package com.etread.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParagraphCommentGroupVO {

    private String paragraphId;

    private List<ParagraphCommentVO> comments = new ArrayList<>();

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public List<ParagraphCommentVO> getComments() {
        return comments;
    }

    public void setComments(List<ParagraphCommentVO> comments) {
        this.comments = comments;
    }
}
