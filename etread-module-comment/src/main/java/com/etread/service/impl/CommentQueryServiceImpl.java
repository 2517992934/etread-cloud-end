package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etread.dto.ChapterCommentQueryDTO;
import com.etread.entity.BookParagraphComment;
import com.etread.mapper.BookParagraphCommentMapper;
import com.etread.service.CommentLikeService;
import com.etread.service.CommentQueryService;
import com.etread.utils.CommentRedisKeyBuilder;
import com.etread.vo.ChapterCommentResultVO;
import com.etread.vo.ParagraphCommentGroupVO;
import com.etread.vo.ParagraphCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentQueryServiceImpl implements CommentQueryService {

    @Autowired
    private BookParagraphCommentMapper commentMapper;

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ChapterCommentResultVO queryChapterComments(ChapterCommentQueryDTO dto) {
        List<BookParagraphComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<BookParagraphComment>()
                        .eq(BookParagraphComment::getBookId, dto.getBookId())
                        .eq(BookParagraphComment::getChapterId, dto.getChapterId())
                        .orderByAsc(BookParagraphComment::getCreateTime, BookParagraphComment::getId)
        );

        if (comments == null || comments.isEmpty()) {
            return buildEmptyResult(dto);
        }

        Map<Long, ParagraphCommentVO> commentMap = buildCommentMap(dto, comments);
        Map<Long, ParagraphCommentVO> rootMap = buildCommentTree(commentMap);
        List<ParagraphCommentGroupVO> grouped = groupByParagraph(rootMap);
        List<ParagraphCommentVO> hotComments = buildHotComments(dto, commentMap);

        ChapterCommentResultVO result = new ChapterCommentResultVO();
        result.setParagraphGroups(grouped);
        result.setHotComments(hotComments);
        return result;
    }

    private ChapterCommentResultVO buildEmptyResult(ChapterCommentQueryDTO dto) {
        ChapterCommentResultVO result = new ChapterCommentResultVO();
        List<ParagraphCommentVO> hotComments = buildHotComments(dto, Collections.emptyMap());
        result.setHotComments(hotComments);
        return result;
    }

    private Map<Long, ParagraphCommentVO> buildCommentMap(ChapterCommentQueryDTO dto,
                                                          List<BookParagraphComment> comments) {
        List<String> commentIdStrings = comments.stream()
                .map(BookParagraphComment::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());

        String likeCountKey = CommentRedisKeyBuilder.likeCountKey(dto.getChapterId());
        List<Object> hashKeys = new ArrayList<>(commentIdStrings);
        List<Object> cachedCounts = hashKeys.isEmpty()
                ? Collections.emptyList()
                : stringRedisTemplate.opsForHash().multiGet(likeCountKey, hashKeys);

        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (int i = 0; i < commentIdStrings.size(); i++) {
            Object value = cachedCounts == null ? null : cachedCounts.get(i);
            if (value != null) {
                countMap.put(commentIdStrings.get(i), Integer.parseInt(value.toString()));
            }
        }

        Map<String, Boolean> likedMap = fetchLikedMap(dto.getUserId(), commentIdStrings);

        Map<Long, ParagraphCommentVO> result = new LinkedHashMap<>();
        for (BookParagraphComment comment : comments) {
            ParagraphCommentVO vo = new ParagraphCommentVO();
            vo.setId(comment.getId());
            vo.setBookId(comment.getBookId());
            vo.setChapterId(comment.getChapterId());
            vo.setParagraphId(comment.getParagraphId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setParentId(comment.getParentId());
            vo.setReplyToUserId(comment.getReplyToUserId());
            vo.setCreateTime(comment.getCreateTime());

            String commentId = comment.getId() == null ? null : String.valueOf(comment.getId());
            Integer likeCount = commentId == null ? 0 : countMap.getOrDefault(
                    commentId,
                    comment.getLikeCount() == null ? 0 : comment.getLikeCount()
            );
            vo.setLikeCount(likeCount);
            vo.setLiked(commentId != null && Boolean.TRUE.equals(likedMap.get(commentId)));

            result.put(comment.getId(), vo);
        }

        return result;
    }

    private Map<String, Boolean> fetchLikedMap(Long userId, List<String> commentIdStrings) {
        if (userId == null || commentIdStrings == null || commentIdStrings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> results = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
            for (String commentId : commentIdStrings) {
                String key = CommentRedisKeyBuilder.likedUsersKey(Long.valueOf(commentId));
                connection.sIsMember(
                        key.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(userId).getBytes(StandardCharsets.UTF_8)
                );
            }
            return null;
        });

        Map<String, Boolean> likedMap = new LinkedHashMap<>();
        for (int i = 0; i < commentIdStrings.size(); i++) {
            Object value = results == null ? null : results.get(i);
            likedMap.put(commentIdStrings.get(i), value instanceof Boolean && (Boolean) value);
        }
        return likedMap;
    }

    private Map<Long, ParagraphCommentVO> buildCommentTree(Map<Long, ParagraphCommentVO> commentMap) {
        Map<Long, ParagraphCommentVO> roots = new LinkedHashMap<>();

        for (ParagraphCommentVO vo : commentMap.values()) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.put(vo.getId(), vo);
                continue;
            }

            ParagraphCommentVO parent = commentMap.get(parentId);
            if (parent == null) {
                roots.put(vo.getId(), vo);
                continue;
            }
            parent.getReplies().add(vo);
        }

        return roots;
    }

    private List<ParagraphCommentGroupVO> groupByParagraph(Map<Long, ParagraphCommentVO> rootMap) {
        Map<String, ParagraphCommentGroupVO> groupMap = new LinkedHashMap<>();

        for (ParagraphCommentVO root : rootMap.values()) {
            String paragraphId = root.getParagraphId();
            if (paragraphId == null) {
                paragraphId = "";
            }
            ParagraphCommentGroupVO group = groupMap.get(paragraphId);
            if (group == null) {
                group = new ParagraphCommentGroupVO();
                group.setParagraphId(paragraphId);
                groupMap.put(paragraphId, group);
            }
            group.getComments().add(root);
        }

        return new ArrayList<>(groupMap.values());
    }

    private List<ParagraphCommentVO> buildHotComments(ChapterCommentQueryDTO dto,
                                                      Map<Long, ParagraphCommentVO> commentMap) {
        List<Long> hotIds = commentLikeService.listHotCommentIds(dto.getChapterId(), dto.getHotLimit());
        if (hotIds == null || hotIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ParagraphCommentVO> result = new ArrayList<>();
        for (Long hotId : hotIds) {
            ParagraphCommentVO vo = commentMap == null ? null : commentMap.get(hotId);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }
}
