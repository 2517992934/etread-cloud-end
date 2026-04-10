package com.etread.service;

import com.etread.dto.CommentLikeDTO;
import com.etread.vo.CommentLikeVO;

import java.util.List;

public interface CommentLikeService {

    CommentLikeVO likeComment(CommentLikeDTO dto);

    CommentLikeVO unlikeComment(CommentLikeDTO dto);

    boolean hasLiked(Long chapterId, Long commentId, Long userId);

    int getLikeCount(Long chapterId, Long commentId);

    List<Long> listHotCommentIds(Long chapterId, Integer limit);
}
