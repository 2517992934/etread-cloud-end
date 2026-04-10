package com.etread.vo;

import java.util.ArrayList;
import java.util.List;

public class ChapterCommentResultVO {

    private List<ParagraphCommentGroupVO> paragraphGroups = new ArrayList<>();

    private List<ParagraphCommentVO> hotComments = new ArrayList<>();

    public List<ParagraphCommentGroupVO> getParagraphGroups() {
        return paragraphGroups;
    }

    public void setParagraphGroups(List<ParagraphCommentGroupVO> paragraphGroups) {
        this.paragraphGroups = paragraphGroups;
    }

    public List<ParagraphCommentVO> getHotComments() {
        return hotComments;
    }

    public void setHotComments(List<ParagraphCommentVO> hotComments) {
        this.hotComments = hotComments;
    }
}
