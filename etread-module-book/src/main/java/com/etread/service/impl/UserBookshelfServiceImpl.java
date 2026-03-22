package com.etread.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.UserBookshelf;
import com.etread.mapper.SysUserMapper;
import com.etread.mapper.UserBookshelfMapper;
import com.etread.service.UserBookshelfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UserBookshelfServiceImpl extends ServiceImpl<UserBookshelfMapper, UserBookshelf> implements UserBookshelfService {

    @Autowired
    private SysUserMapper sysUserMapper;

    private Long resolveUserId(String account) {
        if (account == null || account.isEmpty()) {
            throw new RuntimeException("account 不能为空");
        }
        Long userId = sysUserMapper.selectUserIdByAccount(account);
        if (userId == null) {
            throw new RuntimeException("account 不存在: " + account);
        }
        return userId;
    }

    @Override
    public boolean addToShelf(Long userId, Long bookId) {
        LambdaQueryWrapper<UserBookshelf> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBookshelf::getUserId, userId).eq(UserBookshelf::getBookId, bookId);
        UserBookshelf exists = this.getOne(qw);
        if (exists != null) {
            return true;
        }
        UserBookshelf row = new UserBookshelf();
        row.setUserId(userId);
        row.setBookId(bookId);
        row.setIsTop(0);
        row.setReadPercentage(0f);
        row.setLastReadTime(new Date());
        return this.save(row);
    }

    @Override
    public boolean addToShelf(String account, Long bookId) {
        return addToShelf(resolveUserId(account), bookId);
    }

    @Override
    public boolean removeFromShelf(Long userId, Long bookId) {
        return this.lambdaUpdate()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getBookId, bookId)
                .remove();
    }

    @Override
    public boolean removeFromShelf(String account, Long bookId) {
        return removeFromShelf(resolveUserId(account), bookId);
    }

    @Override
    public boolean updateProgress(Long userId, Long bookId, Long currentChapterId, Float readPercentage) {
        return this.lambdaUpdate()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getBookId, bookId)
                .set(currentChapterId != null, UserBookshelf::getCurrentChapterId, currentChapterId)
                .set(readPercentage != null, UserBookshelf::getReadPercentage, readPercentage)
                .set(UserBookshelf::getLastReadTime, new Date())
                .update();
    }

    @Override
    public boolean updateProgress(String account, Long bookId, Long currentChapterId, Float readPercentage) {
        return updateProgress(resolveUserId(account), bookId, currentChapterId, readPercentage);
    }

    @Override
    public boolean setTop(Long userId, Long bookId, Integer isTop) {
        int v = (isTop != null && isTop == 1) ? 1 : 0;
        return this.lambdaUpdate()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getBookId, bookId)
                .set(UserBookshelf::getIsTop, v)
                .update();
    }

    @Override
    public boolean setTop(String account, Long bookId, Integer isTop) {
        return setTop(resolveUserId(account), bookId, isTop);
    }

    @Override
    public List<UserBookshelf> listShelf(Long userId) {
        return this.lambdaQuery()
                .eq(UserBookshelf::getUserId, userId)
                .orderByDesc(UserBookshelf::getIsTop)
                .orderByDesc(UserBookshelf::getUpdateTime)
                .list();
    }

    @Override
    public List<UserBookshelf> listShelf(String account) {
        return listShelf(resolveUserId(account));
    }
}
