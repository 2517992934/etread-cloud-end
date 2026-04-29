package com.etread.service;

import com.etread.vo.BookHotListVO;
import com.etread.vo.BookRecommendVO;

import java.util.List;

public interface BookHotService {
    void incrementHotScore(Long bookId, Double score);
    List<BookHotListVO> getHotList(Integer limit);
    List<BookRecommendVO> getRandomRecommend(Integer limit);
    void initPublishedBooksCache();
}
