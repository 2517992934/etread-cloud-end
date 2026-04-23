package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.ParagraphOperationDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookChapterContent;
import com.etread.entity.BookInfo;
import com.etread.mapper.BookChapterContentMapper;
import com.etread.mapper.BookChapterMapper;
import com.etread.mapper.BookInfoMapper;
import com.etread.service.IAuthorService;
import com.etread.utils.IdGenerator;
import com.etread.utils.HtmlContentUtil;
import com.etread.utils.MinioUtil;
import com.etread.vo.ParagraphOperationResultVO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthorServiceImpl implements IAuthorService {

    private static final int BOOK_STATUS_DRAFT = -1;
    private static final String CHAPTER_EDIT_LOCK_PREFIX = "chapter:edit:";
    private static final String BOOK_CHAPTER_SORT_LOCK_PREFIX = "book:chapter:sort:";
    private static final String EDITOR_IMAGE_BUCKET = "contentpicture";

    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Autowired
    private BookChapterMapper bookChapterMapper;

    @Autowired
    private BookChapterContentMapper bookChapterContentMapper;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private HtmlContentUtil htmlContentUtil;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookInfo createBookDraft(AuthorBookCreateDTO dto, Long authorUserId) {
        if (authorUserId == null) {
            throw new RuntimeException("当前作者身份无效");
        }

        BookInfo bookInfo = new BookInfo();
        bookInfo.setTitle(dto.getTitle().trim());
        bookInfo.setAuthor(dto.getAuthor().trim());
        bookInfo.setCoverUrl(dto.getCoverUrl());
        bookInfo.setDescription(dto.getDescription());
        bookInfo.setTags(joinTags(dto.getTags()));
        bookInfo.setPublisher(authorUserId);
        bookInfo.setStatus(BOOK_STATUS_DRAFT);
        bookInfo.setErrorMsg(null);
        bookInfo.setWordCount(0);
        bookInfo.setTotalScore(0);
        bookInfo.setRatingCount(0);
        bookInfo.setOriginalFileUrl(null);

        bookInfoMapper.insert(bookInfo);
        return bookInfoMapper.selectById(bookInfo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookChapter createChapter(AuthorChapterCreateDTO dto, Long authorUserId) {
        BookInfo bookInfo = requireOwnedBook(dto.getBookId(), authorUserId);

        BookChapter chapter = new BookChapter();
        chapter.setBookId(dto.getBookId());
        chapter.setChapterTitle(dto.getChapterTitle().trim());
        chapter.setSortOrder(calculateNextSortOrder(dto.getBookId()));
        chapter.setWordCount(0);
        bookChapterMapper.insert(chapter);

        BookChapterContent chapterContent = new BookChapterContent();
        chapterContent.setChapterId(chapter.getId());
        chapterContent.setContent(normalizeWholeHtml("<p></p>"));
        bookChapterContentMapper.insert(chapterContent);

        refreshBookWordCount(bookInfo.getId());
        return bookChapterMapper.selectById(chapter.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookChapter insertChapter(AuthorChapterInsertDTO dto, Long authorUserId) {
        BookInfo bookInfo = requireOwnedBook(dto.getBookId(), authorUserId);

        RLock lock = redissonClient.getLock(BOOK_CHAPTER_SORT_LOCK_PREFIX + dto.getBookId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("当前书籍章节正在重排，请稍后再试");
            }

            BookChapter anchorChapter = bookChapterMapper.selectById(dto.getAnchorChapterId());
            if (anchorChapter == null) {
                throw new RuntimeException("锚点章节不存在");
            }
            if (!Objects.equals(anchorChapter.getBookId(), dto.getBookId())) {
                throw new RuntimeException("锚点章节不属于当前书籍");
            }
            if (anchorChapter.getSortOrder() == null) {
                throw new RuntimeException("锚点章节排序号异常");
            }

            String position = dto.getPosition().trim().toUpperCase(Locale.ROOT);
            int newSortOrder;
            if ("AFTER".equals(position)) {
                newSortOrder = anchorChapter.getSortOrder() + 100;
            } else if ("BEFORE".equals(position)) {
                newSortOrder = anchorChapter.getSortOrder();
            } else {
                throw new RuntimeException("position 仅支持 BEFORE 或 AFTER");
            }

            // 多米诺平移：为插入位置腾出完整的 100 间距，避免序号冲突
            bookChapterMapper.shiftSortOrders(dto.getBookId(), newSortOrder);

            BookChapter chapter = new BookChapter();
            chapter.setId(IdGenerator.generatechapterid(dto.getBookId(), newSortOrder));
            chapter.setBookId(dto.getBookId());
            chapter.setChapterTitle(dto.getChapterTitle().trim());
            chapter.setSortOrder(newSortOrder);
            chapter.setWordCount(0);
            bookChapterMapper.insert(chapter);

            BookChapterContent chapterContent = new BookChapterContent();
            chapterContent.setChapterId(chapter.getId());
            chapterContent.setContent(normalizeWholeHtml("<p></p>"));
            bookChapterContentMapper.insert(chapterContent);

            refreshBookWordCount(bookInfo.getId());
            return bookChapterMapper.selectById(chapter.getId());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapter(AuthorChapterDeleteDTO dto, Long authorUserId) {
        BookChapter chapter = bookChapterMapper.selectById(dto.getChapterId());
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }

        BookInfo bookInfo = requireOwnedBook(chapter.getBookId(), authorUserId);

        RLock lock = redissonClient.getLock(BOOK_CHAPTER_SORT_LOCK_PREFIX + chapter.getBookId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("当前书籍章节正在重排，请稍后再试");
            }

            int chapterWordCount = chapter.getWordCount() == null ? 0 : chapter.getWordCount();

            bookChapterContentMapper.deleteById(dto.getChapterId());
            bookChapterMapper.deleteById(dto.getChapterId());

            int currentBookWordCount = bookInfo.getWordCount() == null ? 0 : bookInfo.getWordCount();
            int newBookWordCount = Math.max(0, currentBookWordCount - chapterWordCount);

            BookInfo update = new BookInfo();
            update.setId(bookInfo.getId());
            update.setWordCount(newBookWordCount);
            bookInfoMapper.updateById(update);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String uploadEditorImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        return minioUtil.uploadFile(file, EDITOR_IMAGE_BUCKET);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParagraphOperationResultVO operateParagraph(ParagraphOperationDTO dto, Long authorUserId) throws Exception {
        if (dto.getChapterId() == null) {
            throw new RuntimeException("chapterId 不能为空");
        }
        if (!StringUtils.hasText(dto.getAction())) {
            throw new RuntimeException("action 不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetParagraphId())) {
            throw new RuntimeException("targetParagraphId 不能为空");
        }

        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        RLock lock = redissonClient.getLock(CHAPTER_EDIT_LOCK_PREFIX + dto.getChapterId());
        boolean locked = false;

        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("当前章节正在被其他设备编辑，请稍后再试");
            }

            BookChapter chapter = bookChapterMapper.selectById(dto.getChapterId());
            if (chapter == null) {
                throw new RuntimeException("章节不存在");
            }

            BookInfo bookInfo = requireOwnedBook(chapter.getBookId(), authorUserId);

            BookChapterContent contentRow = bookChapterContentMapper.selectById(dto.getChapterId());
            if (contentRow == null) {
                contentRow = new BookChapterContent();
                contentRow.setChapterId(dto.getChapterId());
                contentRow.setContent(normalizeWholeHtml("<p></p>"));
                bookChapterContentMapper.insert(contentRow);
            }

            String currentHtml = StringUtils.hasText(contentRow.getContent())
                    ? contentRow.getContent()
                    : normalizeWholeHtml("<p></p>");

            Document document = Jsoup.parseBodyFragment(currentHtml);
            Element body = document.body();
            Element target = body.getElementById(dto.getTargetParagraphId());
            if (target == null) {
                throw new RuntimeException("目标段落不存在，可能已被其他设备修改");
            }

            switch (action) {
                case "UPDATE" -> applyNewContentToParagraph(target, dto.getNewText());
                case "INSERT_AFTER" -> target.after(buildParagraphElement(dto.getNewText()).outerHtml());
                case "DELETE" -> target.remove();
                default -> throw new RuntimeException("不支持的 action：" + dto.getAction());
            }

            if (body.select("p").isEmpty()) {
                body.append(normalizeWholeHtml("<p></p>"));
            }

            String mergedHtml = normalizeWholeHtml(body.html());
            int chapterWordCount = countPureTextWordCount(mergedHtml);

            BookChapterContent updateContent = new BookChapterContent();
            updateContent.setChapterId(chapter.getId());
            updateContent.setContent(mergedHtml);
            bookChapterContentMapper.updateById(updateContent);

            BookChapter updateChapter = new BookChapter();
            updateChapter.setId(chapter.getId());
            updateChapter.setWordCount(chapterWordCount);
            bookChapterMapper.updateById(updateChapter);

            Integer bookWordCount = refreshBookWordCount(bookInfo.getId());

            ParagraphOperationResultVO result = new ParagraphOperationResultVO();
            result.setChapterId(chapter.getId());
            result.setContent(mergedHtml);
            result.setChapterWordCount(chapterWordCount);
            result.setBookWordCount(bookWordCount);
            return result;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private BookInfo requireOwnedBook(Long bookId, Long authorUserId) {
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        if (bookInfo == null) {
            throw new RuntimeException("书籍不存在");
        }
        if (!Objects.equals(bookInfo.getPublisher(), authorUserId)) {
            throw new RuntimeException("你无权操作这本书");
        }
        return bookInfo;
    }

    private Integer calculateNextSortOrder(Long bookId) {
        QueryWrapper<BookChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId).orderByDesc("sort_order").last("limit 1");
        BookChapter lastChapter = bookChapterMapper.selectOne(wrapper);
        if (lastChapter == null || lastChapter.getSortOrder() == null) {
            return 1;
        }
        return lastChapter.getSortOrder() + 1;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> cleaned = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return null;
        }
        return String.join(",", cleaned);
    }

    private void applyNewContentToParagraph(Element target, String newText) {
        if (!StringUtils.hasText(newText)) {
            target.empty();
            applyParagraphId(target);
            return;
        }
        if (looksLikeHtmlFragment(newText)) {
            target.html(newText);
        } else {
            target.text(newText);
        }
        applyParagraphId(target);
    }

    private Element buildParagraphElement(String newText) {
        Document doc = Jsoup.parseBodyFragment("<p></p>");
        Element p = doc.selectFirst("p");
        if (p == null) {
            throw new RuntimeException("构造段落节点失败");
        }
        if (StringUtils.hasText(newText)) {
            if (looksLikeHtmlFragment(newText)) {
                p.html(newText);
            } else {
                p.text(newText);
            }
        }
        applyParagraphId(p);
        return p;
    }

    private void applyParagraphId(Element paragraph) {
        String normalized = htmlContentUtil.injectMd5ToHtml(paragraph.outerHtml());
        Document normalizedDoc = Jsoup.parseBodyFragment(normalized);
        Element normalizedP = normalizedDoc.selectFirst("p");
        if (normalizedP != null) {
            paragraph.html(normalizedP.html());
            if (StringUtils.hasText(normalizedP.id())) {
                paragraph.attr("id", normalizedP.id());
                return;
            }
        }
        String fallbackSource = paragraph.html();
        paragraph.attr("id", DigestUtils.md5DigestAsHex(fallbackSource.getBytes(StandardCharsets.UTF_8)));
    }

    private String normalizeWholeHtml(String html) {
        String normalized = htmlContentUtil.injectMd5ToHtml(html == null ? "" : html);
        Document doc = Jsoup.parseBodyFragment(normalized);
        for (Element p : doc.select("p")) {
            if (!StringUtils.hasText(p.id())) {
                String fallbackSource = p.html();
                p.attr("id", DigestUtils.md5DigestAsHex(fallbackSource.getBytes(StandardCharsets.UTF_8)));
            }
        }
        return doc.body().html();
    }

    private int countPureTextWordCount(String html) {
        String plainText = Jsoup.parseBodyFragment(html == null ? "" : html).text();
        String compact = plainText.replaceAll("\\s+", "");
        return compact.length();
    }

    private Integer refreshBookWordCount(Long bookId) {
        Long total = bookChapterMapper.sumWordCountByBookId(bookId);
        int wordCount = total == null ? 0 : total.intValue();
        BookInfo update = new BookInfo();
        update.setId(bookId);
        update.setWordCount(wordCount);
        bookInfoMapper.updateById(update);
        return wordCount;
    }

    private boolean looksLikeHtmlFragment(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("<") && trimmed.endsWith(">");
    }
}
