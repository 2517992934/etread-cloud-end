package com.etread.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.BookChapter;
import com.etread.mapper.BookChapterMapper;
import com.etread.service.BookChapterService;
import org.springframework.stereotype.Service;

/**
 * 章节目录服务实现
 * 核心功能：批量保存章节信息、查询目录列表
 */
@Service
public class BookChapterServiceImpl extends ServiceImpl<BookChapterMapper, BookChapter> implements BookChapterService {


}