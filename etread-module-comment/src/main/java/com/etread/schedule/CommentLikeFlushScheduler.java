package com.etread.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.etread.constant.CommentRedisKeyConstants;
import com.etread.entity.BookParagraphComment;
import com.etread.entity.CommentLikeRecord;
import com.etread.mapper.BookParagraphCommentMapper;
import com.etread.mapper.CommentLikeRecordMapper;
import com.etread.utils.CommentRedisKeyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Component
public class CommentLikeFlushScheduler {

    private static final long DEFAULT_DELAY_MS = 10000L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BookParagraphCommentMapper commentMapper;

    @Autowired
    private CommentLikeRecordMapper commentLikeRecordMapper;

    @Scheduled(fixedDelay = DEFAULT_DELAY_MS)
    public void flushCommentLikeData() {
        Set<String> chapterIdSet = stringRedisTemplate.opsForSet()
                .members(CommentRedisKeyConstants.COMMENT_LIKE_CHANGED_CHAPTERS_KEY);
        if (chapterIdSet == null || chapterIdSet.isEmpty()) {
            return;
        }

        for (String chapterIdStr : chapterIdSet) {
            if (chapterIdStr == null || chapterIdStr.isBlank()) {
                continue;
            }
            Long chapterId = Long.valueOf(chapterIdStr);
            flushChapter(chapterId);
        }
    }

    private void flushChapter(Long chapterId) {
        String changedCommentsKey = CommentRedisKeyBuilder.changedCommentsKey(chapterId);
        Set<String> commentIdSet = stringRedisTemplate.opsForSet().members(changedCommentsKey);
        if (commentIdSet == null || commentIdSet.isEmpty()) {
            stringRedisTemplate.opsForSet().remove(
                    CommentRedisKeyConstants.COMMENT_LIKE_CHANGED_CHAPTERS_KEY,
                    String.valueOf(chapterId)
            );
            return;
        }

        for (String commentIdStr : commentIdSet) {
            if (commentIdStr == null || commentIdStr.isBlank()) {
                continue;
            }
            Long commentId = Long.valueOf(commentIdStr);
            flushSingleComment(chapterId, commentId);
            stringRedisTemplate.opsForSet().remove(changedCommentsKey, commentIdStr);
        }

        Set<String> remaining = stringRedisTemplate.opsForSet().members(changedCommentsKey);
        if (remaining == null || remaining.isEmpty()) {
            stringRedisTemplate.opsForSet().remove(
                    CommentRedisKeyConstants.COMMENT_LIKE_CHANGED_CHAPTERS_KEY,
                    String.valueOf(chapterId)
            );
        }
    }

    private void flushSingleComment(Long chapterId, Long commentId) {
        String likeCountKey = CommentRedisKeyBuilder.likeCountKey(chapterId);
        Object countValue = stringRedisTemplate.opsForHash().get(likeCountKey, String.valueOf(commentId));
        int likeCount = countValue == null ? 0 : Integer.parseInt(countValue.toString());

        LambdaUpdateWrapper<BookParagraphComment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BookParagraphComment::getId, commentId)
                .set(BookParagraphComment::getLikeCount, likeCount);
        commentMapper.update(null, updateWrapper);

        flushPendingAdd(commentId);
        flushPendingRemove(commentId);
    }

    private void flushPendingAdd(Long commentId) {
        String pendingAddKey = CommentRedisKeyBuilder.pendingAddKey(commentId);
        Set<String> userIdSet = stringRedisTemplate.opsForSet().members(pendingAddKey);
        if (userIdSet == null || userIdSet.isEmpty()) {
            stringRedisTemplate.delete(pendingAddKey);
            return;
        }

        for (String userIdStr : userIdSet) {
            if (userIdStr == null || userIdStr.isBlank()) {
                continue;
            }
            CommentLikeRecord record = new CommentLikeRecord();
            record.setUserId(Long.valueOf(userIdStr));
            record.setCommentId(commentId);
            try {
                commentLikeRecordMapper.insert(record);
            } catch (Exception ignore) {
                // Unique index will block duplicates; ignore insert errors.
            }
        }

        stringRedisTemplate.delete(pendingAddKey);
    }

    private void flushPendingRemove(Long commentId) {
        String pendingRemoveKey = CommentRedisKeyBuilder.pendingRemoveKey(commentId);
        Set<String> userIdSet = stringRedisTemplate.opsForSet().members(pendingRemoveKey);
        if (userIdSet == null || userIdSet.isEmpty()) {
            stringRedisTemplate.delete(pendingRemoveKey);
            return;
        }

        for (String userIdStr : userIdSet) {
            if (userIdStr == null || userIdStr.isBlank()) {
                continue;
            }
            Long userId = Long.valueOf(userIdStr);
            commentLikeRecordMapper.delete(
                    new LambdaQueryWrapper<CommentLikeRecord>()
                            .eq(CommentLikeRecord::getCommentId, commentId)
                            .eq(CommentLikeRecord::getUserId, userId)
            );
        }

        stringRedisTemplate.delete(pendingRemoveKey);
    }
}
