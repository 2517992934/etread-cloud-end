package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.entity.BookInfo;
import com.etread.entity.UserBookshelf;

import java.util.List;

public interface UserBookshelfService extends IService<UserBookshelf> {
    boolean addToShelf(Long userId, Long bookId);
    boolean addToShelf(String account, Long bookId);
    boolean removeFromShelf(Long userId, Long bookId);
    boolean removeFromShelf(String account, Long bookId);
    boolean updateProgress(Long userId, Long bookId, Long currentChapterId, Float readPercentage);
    boolean updateProgress(String account, Long bookId, Long currentChapterId, Float readPercentage);
    boolean setTop(Long userId, Long bookId, Integer isTop);
    boolean setTop(String account, Long bookId, Integer isTop);
    List<UserBookshelf> listShelf(Long userId);
    List<UserBookshelf> listShelf(String account);
    List<BookInfo> listSubscribedBooksByUserId(Long userId);
}
