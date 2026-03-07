package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.BookChapter;
import com.etread.mapper.BookChapterMapper;
import com.etread.service.BookChapterContentService;
import com.etread.service.BookChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 章节目录服务实现
 * 核心功能：批量保存章节信息、查询目录列表
 */
@Service
public class BookChapterServiceImpl extends ServiceImpl<BookChapterMapper, BookChapter> implements BookChapterService {

    @Autowired
    private BookChapterContentService bookChapterContentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByBookId(Long bookId) {
        // 1. 查询该书所有章节 ID
        LambdaQueryWrapper<BookChapter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookChapter::getBookId, bookId)
                    .select(BookChapter::getId);
        
        List<Object> ids = this.baseMapper.selectObjs(queryWrapper);
        
        if (ids != null && !ids.isEmpty()) {
            // 转换为 Long 类型列表
            List<Long> chapterIds = ids.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());

            // 2. 级联删除章节内容
            bookChapterContentService.removeByChapterIds(chapterIds);
            
            // 3. 删除章节本身
            LambdaQueryWrapper<BookChapter> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(BookChapter::getBookId, bookId);
            return this.remove(deleteWrapper);
        }
        
        return true;
    }
}