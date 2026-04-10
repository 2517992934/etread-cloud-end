package com.etread.controller;

import com.etread.Result;
import com.etread.component.CommentUserResolver;
import com.etread.dto.ChapterCommentQueryDTO;
import com.etread.dto.ChapterCommentQueryReq;
import com.etread.dto.CommentLikeDTO;
import com.etread.dto.CommentLikeReq;
import com.etread.dto.CommentPublishDTO;
import com.etread.dto.CommentPublishReq;
import com.etread.service.CommentLikeService;
import com.etread.service.CommentPublishService;
import com.etread.service.CommentQueryService;
import com.etread.vo.ChapterCommentResultVO;
import com.etread.vo.CommentLikeVO;
import com.etread.vo.ParagraphCommentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/comment")
@Validated
public class CommentController {

    @Autowired
    private CommentPublishService commentPublishService;

    @Autowired
    private CommentUserResolver commentUserResolver;

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private CommentQueryService commentQueryService;

    @PostMapping("/publish")
    public Result<ParagraphCommentVO> publish(@RequestHeader("token") String token,
                                             CommentPublishReq req) {
        Long userId = commentUserResolver.requireUserId(token);
        CommentPublishDTO dto = new CommentPublishDTO();
        dto.setBookId(req.getBookId());
        dto.setChapterId(req.getChapterId());
        dto.setParagraphId(req.getParagraphId());
        dto.setUserId(userId);
        dto.setContent(req.getContent());
        dto.setParentId(req.getParentId());
        dto.setReplyToUserId(req.getReplyToUserId());
        ParagraphCommentVO vo = commentPublishService.publish(dto);
        return Result.success("发布成功", vo);
    }

    @PostMapping("/like")
    public Result<CommentLikeVO> like(@RequestHeader("token") String token,
                                      @RequestBody @Valid CommentLikeReq req) {
        Long userId = commentUserResolver.requireUserId(token);
        CommentLikeDTO dto = new CommentLikeDTO();
        dto.setChapterId(req.getChapterId());
        dto.setCommentId(req.getCommentId());
        dto.setUserId(userId);
        CommentLikeVO vo = commentLikeService.likeComment(dto);
        return Result.success("点赞成功", vo);
    }

    @PostMapping("/unlike")
    public Result<CommentLikeVO> unlike(@RequestHeader("token") String token,
                                        @RequestBody @Valid CommentLikeReq req) {
        Long userId = commentUserResolver.requireUserId(token);
        CommentLikeDTO dto = new CommentLikeDTO();
        dto.setChapterId(req.getChapterId());
        dto.setCommentId(req.getCommentId());
        dto.setUserId(userId);
        CommentLikeVO vo = commentLikeService.unlikeComment(dto);
        return Result.success("取消点赞成功", vo);
    }

    @PostMapping("/chapter")
    public Result<ChapterCommentResultVO> queryChapter(@RequestHeader("token") String token,
                                                       @RequestBody @Valid ChapterCommentQueryReq req) {
        Long userId = commentUserResolver.requireUserId(token);
        ChapterCommentQueryDTO dto = new ChapterCommentQueryDTO();
        dto.setBookId(req.getBookId());
        dto.setChapterId(req.getChapterId());
        dto.setUserId(userId);
        dto.setHotLimit(req.getHotLimit());
        ChapterCommentResultVO result = commentQueryService.queryChapterComments(dto);
        return Result.success("查询成功", result);
    }
}
