package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.etread.dto.BookReviewAddDTO;
import com.etread.entity.BookInfo;
import com.etread.entity.BookReview;
import com.etread.mapper.BookInfoMapper;
import com.etread.mapper.BookReviewMapper;
import com.etread.service.BookReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class BookReviewServiceImpl implements BookReviewService {

    @Autowired
    private BookReviewMapper bookReviewMapper;

    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookReview addReview(BookReviewAddDTO dto) {
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 10) {
            throw new IllegalArgumentException("评分必须在 1-10 分之间");
        }

        BookReview review = new BookReview();
        review.setBookId(dto.getBookId());
        review.setUserId(dto.getUserId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setLikeCount(0);
        bookReviewMapper.insert(review);

        LambdaUpdateWrapper<BookInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BookInfo::getId, dto.getBookId())
                .setSql("total_score = total_score + " + dto.getRating())
                .setSql("rating_count = rating_count + 1");
        bookInfoMapper.update(null, updateWrapper);

        return review;
    }

    @Override
    public Page<BookReview> listByBookId(Long bookId, int page, int size) {
        Page<BookReview> p = new Page<>(page, size);
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReview::getBookId, bookId)
                .orderByDesc(BookReview::getCreateTime, BookReview::getId);
        return bookReviewMapper.selectPage(p, wrapper);
    }
}
