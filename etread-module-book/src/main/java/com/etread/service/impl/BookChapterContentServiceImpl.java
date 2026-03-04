package com.etread.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.BookChapterContent;
import com.etread.mapper.BookChapterContentMapper;
import com.etread.service.BookChapterContentService;
import org.springframework.stereotype.Service;

@Service
public class BookChapterContentServiceImpl extends ServiceImpl<BookChapterContentMapper, BookChapterContent> implements BookChapterContentService {
}
