package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etread.dto.CommentLikeDTO;
import com.etread.entity.BookParagraphComment;
import com.etread.entity.CommentLikeRecord;
import com.etread.mapper.BookParagraphCommentMapper;
import com.etread.mapper.CommentLikeRecordMapper;
import com.etread.service.CommentLikeService;
import com.etread.utils.CommentRedisKeyBuilder;
import com.etread.constant.CommentRedisKeyConstants;
import com.etread.vo.CommentLikeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentLikeServiceImpl implements CommentLikeService {

    private static final int DEFAULT_HOT_LIMIT = 3;
    private static final Duration INIT_LOCK_TTL = Duration.ofSeconds(5);
    private static final int INIT_WAIT_RETRY_TIMES = 20;
    private static final long INIT_WAIT_MILLIS = 50L;

    private static final DefaultRedisScript<Long> LIKE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> UNLIKE_SCRIPT = new DefaultRedisScript<>();

    static {
        LIKE_SCRIPT.setResultType(Long.class);
        LIKE_SCRIPT.setScriptText(
                "local added = redis.call('SADD', KEYS[1], ARGV[1]) " +
                "if added == 1 then " +
                "  local newCount = redis.call('HINCRBY', KEYS[2], ARGV[2], 1) " +
                "  redis.call('ZADD', KEYS[3], newCount, ARGV[2]) " +
                "  redis.call('SADD', KEYS[4], ARGV[3]) " +
                "  redis.call('SADD', KEYS[5], ARGV[2]) " +
                "  redis.call('SADD', KEYS[6], ARGV[1]) " +
                "  redis.call('SREM', KEYS[7], ARGV[1]) " +
                "  return newCount " +
                "end " +
                "local current = redis.call('HGET', KEYS[2], ARGV[2]) " +
                "if not current then return 0 end " +
                "return tonumber(current)"
        );

        UNLIKE_SCRIPT.setResultType(Long.class);
        UNLIKE_SCRIPT.setScriptText(
                "local removed = redis.call('SREM', KEYS[1], ARGV[1]) " +
                "local current = tonumber(redis.call('HGET', KEYS[2], ARGV[2]) or '0') " +
                "if removed == 1 then " +
                "  local newCount = current > 0 and (current - 1) or 0 " +
                "  redis.call('HSET', KEYS[2], ARGV[2], newCount) " +
                "  redis.call('ZADD', KEYS[3], newCount, ARGV[2]) " +
                "  redis.call('SADD', KEYS[4], ARGV[3]) " +
                "  redis.call('SADD', KEYS[5], ARGV[2]) " +
                "  redis.call('SADD', KEYS[7], ARGV[1]) " +
                "  redis.call('SREM', KEYS[6], ARGV[1]) " +
                "  return newCount " +
                "end " +
                "return current"
        );
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BookParagraphCommentMapper commentMapper;

    @Autowired
    private CommentLikeRecordMapper commentLikeRecordMapper;

    @Override
    public CommentLikeVO likeComment(CommentLikeDTO dto) {
        validateCommentChapterRelation(dto.getCommentId(), dto.getChapterId());
        ensureCommentLikeCacheLoaded(dto.getChapterId(), dto.getCommentId());

        Long latestCount = stringRedisTemplate.execute(
                LIKE_SCRIPT,
                buildLikeScriptKeys(dto.getChapterId(), dto.getCommentId()),
                String.valueOf(dto.getUserId()),
                String.valueOf(dto.getCommentId()),
                String.valueOf(dto.getChapterId())
        );

        return buildLikeVO(dto.getCommentId(), true, latestCount);
    }

    @Override
    public CommentLikeVO unlikeComment(CommentLikeDTO dto) {
        validateCommentChapterRelation(dto.getCommentId(), dto.getChapterId());
        ensureCommentLikeCacheLoaded(dto.getChapterId(), dto.getCommentId());

        Long latestCount = stringRedisTemplate.execute(
                UNLIKE_SCRIPT,
                buildLikeScriptKeys(dto.getChapterId(), dto.getCommentId()),
                String.valueOf(dto.getUserId()),
                String.valueOf(dto.getCommentId()),
                String.valueOf(dto.getChapterId())
        );

        return buildLikeVO(dto.getCommentId(), false, latestCount);
    }

    @Override
    public boolean hasLiked(Long chapterId, Long commentId, Long userId) {
        validateCommentChapterRelation(commentId, chapterId);
        ensureCommentLikeCacheLoaded(chapterId, commentId);
        String likedUsersKey = CommentRedisKeyBuilder.likedUsersKey(commentId);
        Boolean member = stringRedisTemplate.opsForSet().isMember(likedUsersKey, String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }

    @Override
    public int getLikeCount(Long chapterId, Long commentId) {
        validateCommentChapterRelation(commentId, chapterId);
        ensureCommentLikeCacheLoaded(chapterId, commentId);
        String likeCountKey = CommentRedisKeyBuilder.likeCountKey(chapterId);
        Object value = stringRedisTemplate.opsForHash().get(likeCountKey, String.valueOf(commentId));
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public List<Long> listHotCommentIds(Long chapterId, Integer limit) {
        ensureChapterHotCacheLoaded(chapterId);

        int hotLimit = (limit == null || limit <= 0) ? DEFAULT_HOT_LIMIT : limit;
        Set<String> commentIdSet = stringRedisTemplate.opsForZSet()
                .reverseRange(CommentRedisKeyBuilder.hotKey(chapterId), 0, hotLimit - 1L);

        if (commentIdSet == null || commentIdSet.isEmpty()) {
            return Collections.emptyList();
        }

        return commentIdSet.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private void ensureCommentLikeCacheLoaded(Long chapterId, Long commentId) {
        String loadedKey = CommentRedisKeyBuilder.likeCacheLoadedKey(commentId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loadedKey))) {
            return;
        }

        String lockKey = CommentRedisKeyBuilder.likeCacheInitLockKey(commentId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", INIT_LOCK_TTL);
        if (Boolean.TRUE.equals(locked)) {
            try {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loadedKey))) {
                    return;
                }

                BookParagraphComment comment = commentMapper.selectById(commentId);
                if (comment == null) {
                    throw new IllegalArgumentException("评论不存在，commentId=" + commentId);
                }
                if (!Objects.equals(comment.getChapterId(), chapterId)) {
                    throw new IllegalArgumentException("评论和章节不匹配，commentId=" + commentId);
                }

                Integer dbLikeCount = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
                stringRedisTemplate.opsForHash().putIfAbsent(
                        CommentRedisKeyBuilder.likeCountKey(chapterId),
                        String.valueOf(commentId),
                        String.valueOf(dbLikeCount)
                );

                List<CommentLikeRecord> likeRecords = commentLikeRecordMapper.selectList(
                        new LambdaQueryWrapper<CommentLikeRecord>()
                                .eq(CommentLikeRecord::getCommentId, commentId)
                );
                if (!likeRecords.isEmpty()) {
                    String[] userIdArray = likeRecords.stream()
                            .map(CommentLikeRecord::getUserId)
                            .filter(Objects::nonNull)
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    if (userIdArray.length > 0) {
                        stringRedisTemplate.opsForSet().add(
                                CommentRedisKeyBuilder.likedUsersKey(commentId),
                                userIdArray
                        );
                    }
                }

                stringRedisTemplate.opsForValue().set(loadedKey, "1");
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
            return;
        }

        waitUntilLoaded(loadedKey);
    }

    private void ensureChapterHotCacheLoaded(Long chapterId) {
        String loadedKey = CommentRedisKeyBuilder.hotCacheLoadedKey(chapterId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loadedKey))) {
            return;
        }

        String lockKey = CommentRedisKeyBuilder.hotCacheInitLockKey(chapterId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", INIT_LOCK_TTL);
        if (Boolean.TRUE.equals(locked)) {
            try {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loadedKey))) {
                    return;
                }

                List<BookParagraphComment> commentList = commentMapper.selectList(
                        new LambdaQueryWrapper<BookParagraphComment>()
                                .eq(BookParagraphComment::getChapterId, chapterId)
                                .select(BookParagraphComment::getId, BookParagraphComment::getLikeCount)
                );

                String likeCountKey = CommentRedisKeyBuilder.likeCountKey(chapterId);
                String hotKey = CommentRedisKeyBuilder.hotKey(chapterId);
                for (BookParagraphComment comment : commentList) {
                    if (comment.getId() == null) {
                        continue;
                    }
                    String commentId = String.valueOf(comment.getId());
                    Object cachedCount = stringRedisTemplate.opsForHash().get(likeCountKey, commentId);
                    double score = cachedCount == null
                            ? (comment.getLikeCount() == null ? 0D : comment.getLikeCount().doubleValue())
                            : Double.parseDouble(cachedCount.toString());
                    stringRedisTemplate.opsForZSet().addIfAbsent(hotKey, commentId, score);
                }

                stringRedisTemplate.opsForValue().set(loadedKey, "1");
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
            return;
        }

        waitUntilLoaded(loadedKey);
    }

    private void validateCommentChapterRelation(Long commentId, Long chapterId) {
        BookParagraphComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在，commentId=" + commentId);
        }
        if (!Objects.equals(comment.getChapterId(), chapterId)) {
            throw new IllegalArgumentException("评论和章节不匹配，commentId=" + commentId);
        }
    }

    private void waitUntilLoaded(String loadedKey) {
        for (int i = 0; i < INIT_WAIT_RETRY_TIMES; i++) {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loadedKey))) {
                return;
            }
            try {
                Thread.sleep(INIT_WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待 Redis 缓存初始化被中断", e);
            }
        }
        throw new IllegalStateException("Redis 缓存初始化超时，key=" + loadedKey);
    }

    private List<String> buildLikeScriptKeys(Long chapterId, Long commentId) {
        List<String> keys = new ArrayList<>(7);
        keys.add(CommentRedisKeyBuilder.likedUsersKey(commentId));
        keys.add(CommentRedisKeyBuilder.likeCountKey(chapterId));
        keys.add(CommentRedisKeyBuilder.hotKey(chapterId));
        keys.add(CommentRedisKeyConstants.COMMENT_LIKE_CHANGED_CHAPTERS_KEY);
        keys.add(CommentRedisKeyBuilder.changedCommentsKey(chapterId));
        keys.add(CommentRedisKeyBuilder.pendingAddKey(commentId));
        keys.add(CommentRedisKeyBuilder.pendingRemoveKey(commentId));
        return keys;
    }

    private CommentLikeVO buildLikeVO(Long commentId, boolean liked, Long latestCount) {
        CommentLikeVO vo = new CommentLikeVO();
        vo.setCommentId(commentId);
        vo.setLiked(liked);
        vo.setLikeCount(latestCount == null ? 0 : latestCount.intValue());
        return vo;
    }
}
