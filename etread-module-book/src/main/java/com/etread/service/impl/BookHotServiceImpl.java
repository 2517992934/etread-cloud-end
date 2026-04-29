package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etread.entity.BookInfo;
import com.etread.mapper.BookInfoMapper;
import com.etread.service.BookHotService;
import com.etread.vo.BookHotListVO;
import com.etread.vo.BookRecommendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookHotServiceImpl implements BookHotService {

    private static final String BOOK_HOT_KEY = "book:hot";
    private static final String PUBLISHED_BOOKS_KEY = "published_books";
    private static final String PUBLISHED_BOOKS_UPDATE_KEY = "published_books:last_update";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Override
    public void incrementHotScore(Long bookId, Double score) {
        if (bookId == null || score == null || score <= 0) {
            log.warn("invalid incrementHotScore params: bookId={}, score={}", bookId, score);
            return;
        }
        stringRedisTemplate.opsForZSet().incrementScore(BOOK_HOT_KEY, bookId.toString(), score);
        log.debug("incremented hot score for bookId={} by {}", bookId, score);
    }

    @Override
    public List<BookHotListVO> getHotList(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        Set<String> hotBookIds = stringRedisTemplate.opsForZSet()
                .reverseRange(BOOK_HOT_KEY, 0, limit - 1);

        if (hotBookIds == null || hotBookIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> bookIds = hotBookIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .in(BookInfo::getId, bookIds)
                        .eq(BookInfo::getStatus, 1)
        );

        Map<Long, Integer> idIndexMap = new HashMap<>();
        for (int i = 0; i < bookIds.size(); i++) {
            idIndexMap.put(bookIds.get(i), i);
        }

        books.sort((a, b) -> {
            Integer idxA = idIndexMap.get(a.getId());
            Integer idxB = idIndexMap.get(b.getId());
            return idxA != null && idxB != null ? idxA.compareTo(idxB) : 0;
        });

        List<BookHotListVO> result = new ArrayList<>();
        int rank = 1;
        for (BookInfo book : books) {
            Double hotScore = stringRedisTemplate.opsForZSet().score(BOOK_HOT_KEY, book.getId().toString());

            BookHotListVO vo = new BookHotListVO();
            vo.setRank(rank++);
            vo.setBookId(book.getId());
            vo.setTitle(book.getTitle());
            vo.setAuthor(book.getAuthor());
            vo.setCoverUrl(book.getCoverUrl());
            vo.setTotalScore(book.getTotalScore());
            vo.setRatingCount(book.getRatingCount());
            if (book.getRatingCount() != null && book.getRatingCount() > 0) {
                vo.setAverageScore(book.getTotalScore() / (double) book.getRatingCount());
            } else {
                vo.setAverageScore(0.0);
            }
            vo.setHotScore(hotScore != null ? hotScore : 0.0);
            result.add(vo);
        }

        return result;
    }

    @Override
    public List<BookRecommendVO> getRandomRecommend(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }

        ensurePublishedBooksCache();

        List<String> randomBookIds = stringRedisTemplate.opsForSet()
                .randomMembers(PUBLISHED_BOOKS_KEY, limit);

        if (randomBookIds == null || randomBookIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> bookIds = randomBookIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .in(BookInfo::getId, bookIds)
                        .eq(BookInfo::getStatus, 1)
        );

        return books.stream()
                .map(book -> {
                    BookRecommendVO vo = new BookRecommendVO();
                    vo.setBookId(book.getId());
                    vo.setTitle(book.getTitle());
                    vo.setAuthor(book.getAuthor());
                    vo.setCoverUrl(book.getCoverUrl());
                    vo.setDescription(book.getDescription());
                    vo.setWordCount(book.getWordCount());
                    vo.setTotalScore(book.getTotalScore());
                    vo.setRatingCount(book.getRatingCount());
                    if (book.getRatingCount() != null && book.getRatingCount() > 0) {
                        vo.setAverageScore(book.getTotalScore() / (double) book.getRatingCount());
                    } else {
                        vo.setAverageScore(0.0);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void initPublishedBooksCache() {
        ensurePublishedBooksCache();
    }

    private void ensurePublishedBooksCache() {
        Long cacheSize = stringRedisTemplate.opsForSet().size(PUBLISHED_BOOKS_KEY);
        if (cacheSize != null && cacheSize > 0) {
            log.debug("published_books cache already initialized with {} items", cacheSize);
            return;
        }

        List<BookInfo> publishedBooks = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .eq(BookInfo::getStatus, 1)
                .select(BookInfo::getId)
        );

        if (publishedBooks != null && !publishedBooks.isEmpty()) {
            String[] bookIds = publishedBooks.stream()
                    .map(b -> b.getId().toString())
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(PUBLISHED_BOOKS_KEY, bookIds);
            stringRedisTemplate.opsForValue().set(PUBLISHED_BOOKS_UPDATE_KEY, System.currentTimeMillis() + "");
            log.info("initialized published_books cache with {} books", bookIds.length);
        }
    }
}
