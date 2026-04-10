package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etread.dto.CommentPublishDTO;
import com.etread.entity.BookParagraphComment;
import com.etread.mapper.BookParagraphCommentMapper;
import com.etread.service.CommentPublishService;
import com.etread.utils.CommentRedisKeyBuilder;
import com.etread.vo.ParagraphCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CommentPublishServiceImpl implements CommentPublishService {

    @Autowired
    private BookParagraphCommentMapper commentMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ParagraphCommentVO publish(CommentPublishDTO dto) {
        Long parentId = dto.getParentId();
        if (parentId != null && parentId != 0L) {
            BookParagraphComment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("父评论不存在，parentId=" + parentId);
            }
            if (!parent.getChapterId().equals(dto.getChapterId())) {
                throw new IllegalArgumentException("父评论不属于当前章节");
            }
        }

        BookParagraphComment comment = new BookParagraphComment();
        comment.setBookId(dto.getBookId());
        comment.setChapterId(dto.getChapterId());
        comment.setParagraphId(dto.getParagraphId());
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setParentId(parentId == null ? 0L : parentId);
        comment.setReplyToUserId(dto.getReplyToUserId());
        comment.setCreateTime(new Date());

        commentMapper.insert(comment);

        Long commentId = comment.getId();
        String likeCountKey = CommentRedisKeyBuilder.likeCountKey(dto.getChapterId());
        stringRedisTemplate.opsForHash().putIfAbsent(likeCountKey, String.valueOf(commentId), "0");
        stringRedisTemplate.opsForZSet().addIfAbsent(
                CommentRedisKeyBuilder.hotKey(dto.getChapterId()),
                String.valueOf(commentId),
                0D
        );

        ParagraphCommentVO vo = new ParagraphCommentVO();
        vo.setId(commentId);
        vo.setBookId(comment.getBookId());
        vo.setChapterId(comment.getChapterId());
        vo.setParagraphId(comment.getParagraphId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(0);
        vo.setLiked(false);
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
