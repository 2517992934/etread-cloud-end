package com.etread.service;

import com.etread.dto.BookReviewAddDTO;
import com.etread.entity.BookReview;

public interface BookReviewService {

    BookReview addReview(BookReviewAddDTO dto);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<BookReview> listByBookId(Long bookId, int page, int size);
}
