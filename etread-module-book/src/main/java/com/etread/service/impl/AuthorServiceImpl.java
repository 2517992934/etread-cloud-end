package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorBookPublishDTO;
import com.etread.dto.AuthorBookQueryDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.AuthorChapterQueryDTO;
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
import com.etread.vo.AuthorChapterDetailVO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthorServiceImpl implements IAuthorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorServiceImpl.class);

    private static final int BOOK_STATUS_DRAFT = -1;
    private static final int BOOK_STATUS_PUBLISHED = 1;
    private static final String CHAPTER_EDIT_LOCK_PREFIX = "chapter:edit:";
    private static final String BOOK_CHAPTER_SORT_LOCK_PREFIX = "book:chapter:sort:";
    private static final String EDITOR_IMAGE_BUCKET = "contentpicture";
    private static final String COVER_IMAGE_BUCKET = "contentpicture";

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
    public BookInfo createBookDraft(AuthorBookCreateDTO dto, Long authorUserId, String authorName) {
        if (authorUserId == null) {
            throw new RuntimeException("当前作者身份无效");
        }
        if (!StringUtils.hasText(authorName)) {
            throw new RuntimeException("当前作者昵称无效");
        }
        if (dto.getCover() == null || dto.getCover().isEmpty()) {
            throw new RuntimeException("封面图片不能为空");
        }

        String coverUrl;
        try {
            coverUrl = uploadCoverImage(dto.getCover());
        } catch (Exception e) {
            throw new RuntimeException("封面上传失败", e);
        }

        BookInfo bookInfo = new BookInfo();
        bookInfo.setTitle(dto.getTitle().trim());
        bookInfo.setAuthor(authorName.trim());
        bookInfo.setCoverUrl(coverUrl);
        Long id = IdGenerator.generateId();
        bookInfo.setId(id);
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
    public List<BookInfo> listMyDraftBooks(Long authorUserId) {
        QueryWrapper<BookInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("publisher", authorUserId)
                .eq("status", BOOK_STATUS_DRAFT)
                .orderByDesc("update_time")
                .orderByDesc("id");
        return bookInfoMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookInfo publishBook(AuthorBookPublishDTO dto, Long authorUserId) {
        if (dto.getBookId() == null) {
            throw new RuntimeException("bookId 不能为空");
        }

        BookInfo bookInfo = requireOwnedBook(dto.getBookId(), authorUserId);
        if (!Objects.equals(bookInfo.getStatus(), BOOK_STATUS_DRAFT)) {
            throw new RuntimeException("仅草稿书支持上架");
        }
        if (!StringUtils.hasText(bookInfo.getTitle())) {
            throw new RuntimeException("书名不能为空，无法上架");
        }
        if (!StringUtils.hasText(bookInfo.getAuthor())) {
            throw new RuntimeException("作者名不能为空，无法上架");
        }
        if (!StringUtils.hasText(bookInfo.getCoverUrl())) {
            throw new RuntimeException("封面不能为空，无法上架");
        }

        QueryWrapper<BookChapter> chapterWrapper = new QueryWrapper<>();
        chapterWrapper.eq("book_id", dto.getBookId());
        Long chapterCount = bookChapterMapper.selectCount(chapterWrapper);
        if (chapterCount == null || chapterCount <= 0) {
            throw new RuntimeException("至少创建一章后才能上架");
        }

        refreshBookWordCount(bookInfo.getId());

        BookInfo update = new BookInfo();
        update.setId(bookInfo.getId());
        update.setStatus(BOOK_STATUS_PUBLISHED);
        update.setErrorMsg(null);
        bookInfoMapper.updateById(update);
        return bookInfoMapper.selectById(bookInfo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookChapter createChapter(AuthorChapterCreateDTO dto, Long authorUserId) {
        BookInfo bookInfo = requireOwnedBook(dto.getBookId(), authorUserId);
        RLock lock = redissonClient.getLock(BOOK_CHAPTER_SORT_LOCK_PREFIX + dto.getBookId());
        boolean locked = false;
        try {
            locked = acquireLock(lock, 5, 30, "当前书籍章节正在重排，请稍后再试");
            if (!locked) {
                throw new RuntimeException("当前书籍章节正在重排，请稍后再试");
            }

            BookChapter chapter = new BookChapter();
            chapter.setId(generateNextChapterId(dto.getBookId()));
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
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<BookChapter> listMyBookChapters(AuthorBookQueryDTO dto, Long authorUserId) {
        requireOwnedBook(dto.getBookId(), authorUserId);
        QueryWrapper<BookChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", dto.getBookId())
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return bookChapterMapper.selectList(wrapper);
    }

    @Override
    public AuthorChapterDetailVO getMyChapterDetail(AuthorChapterQueryDTO dto, Long authorUserId) {
        BookChapter chapter = bookChapterMapper.selectById(dto.getChapterId());
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }
        requireOwnedBook(chapter.getBookId(), authorUserId);

        BookChapterContent contentRow = bookChapterContentMapper.selectById(dto.getChapterId());

        AuthorChapterDetailVO detail = new AuthorChapterDetailVO();
        detail.setChapterId(chapter.getId());
        detail.setBookId(chapter.getBookId());
        detail.setChapterTitle(chapter.getChapterTitle());
        detail.setSortOrder(chapter.getSortOrder());
        detail.setWordCount(chapter.getWordCount() == null ? 0 : chapter.getWordCount());
        detail.setContent(contentRow == null ? normalizeWholeHtml("<p></p>") : contentRow.getContent());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookChapter insertChapter(AuthorChapterInsertDTO dto, Long authorUserId) {
        BookInfo bookInfo = requireOwnedBook(dto.getBookId(), authorUserId);

        RLock lock = redissonClient.getLock(BOOK_CHAPTER_SORT_LOCK_PREFIX + dto.getBookId());
        boolean locked = false;
        try {
            locked = acquireLock(lock, 5, 30, "当前书籍章节正在重排，请稍后再试");
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
            chapter.setId(generateNextChapterId(dto.getBookId()));
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
            locked = acquireLock(lock, 5, 30, "当前书籍章节正在重排，请稍后再试");
            if (!locked) {
                throw new RuntimeException("当前书籍章节正在重排，请稍后再试");
            }

            int chapterWordCount = chapter.getWordCount() == null ? 0 : chapter.getWordCount();
            BookChapterContent contentRow = bookChapterContentMapper.selectById(dto.getChapterId());
            Set<String> imagesToDelete = extractManagedImageUrls(
                    contentRow == null ? null : contentRow.getContent(),
                    EDITOR_IMAGE_BUCKET
            );

            bookChapterContentMapper.deleteById(dto.getChapterId());
            bookChapterMapper.deleteById(dto.getChapterId());

            int currentBookWordCount = bookInfo.getWordCount() == null ? 0 : bookInfo.getWordCount();
            int newBookWordCount = Math.max(0, currentBookWordCount - chapterWordCount);

            BookInfo update = new BookInfo();
            update.setId(bookInfo.getId());
            update.setWordCount(newBookWordCount);
            bookInfoMapper.updateById(update);

            scheduleImageDeleteAfterCommit(imagesToDelete, EDITOR_IMAGE_BUCKET);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String uploadEditorImage(Long bookId, MultipartFile file, Long authorUserId) throws Exception {
        if (bookId == null) {
            throw new RuntimeException("bookId 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        requireOwnedBook(bookId, authorUserId);
        String objectName = "books/" + bookId + "/" + normalizeUploadFileName(file);
        return minioUtil.uploadBytes(file.getBytes(), objectName, resolveContentType(file), EDITOR_IMAGE_BUCKET);
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
            locked = acquireLock(lock, 5, 30, "当前章节正在被其他设备编辑，请稍后再试");
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

            Set<String> imagesToDelete = new LinkedHashSet<>();

            switch (action) {
                case "UPDATE" -> {
                    Set<String> oldImages = extractManagedImageUrls(target.outerHtml(), EDITOR_IMAGE_BUCKET);
                    applyNewContentToParagraph(target, dto.getNewText());
                    Set<String> newImages = extractManagedImageUrls(target.outerHtml(), EDITOR_IMAGE_BUCKET);
                    oldImages.removeAll(newImages);
                    imagesToDelete.addAll(oldImages);
                }
                case "INSERT_AFTER" -> target.after(buildParagraphElement(dto.getNewText()).outerHtml());
                case "DELETE" -> {
                    imagesToDelete.addAll(extractManagedImageUrls(target.outerHtml(), EDITOR_IMAGE_BUCKET));
                    target.remove();
                }
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
            scheduleImageDeleteAfterCommit(imagesToDelete, EDITOR_IMAGE_BUCKET);
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

    private Long generateNextChapterId(Long bookId) {
        QueryWrapper<BookChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId).orderByDesc("id").last("limit 1");
        BookChapter lastChapter = bookChapterMapper.selectOne(wrapper);
        if (lastChapter == null || lastChapter.getId() == null) {
            return IdGenerator.generatechapterid(bookId, 100);
        }

        int nextSequence = (int) (lastChapter.getId() % 10000) + 1;
        return IdGenerator.generatechapterid(bookId, nextSequence * 100);
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
        Element p = doc.select("p").first();
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
        Element normalizedP = normalizedDoc.select("p").first();
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

    private String uploadCoverImage(MultipartFile file) throws Exception {
        String objectName = "cover/" + normalizeUploadFileName(file);
        return minioUtil.uploadBytes(file.getBytes(), objectName, resolveContentType(file), COVER_IMAGE_BUCKET);
    }

    private String normalizeUploadFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            return "file-" + System.currentTimeMillis();
        }
        String fileName = originalFilename.trim()
                .replace("\\", "/");
        int lastSlashIndex = fileName.lastIndexOf("/");
        if (lastSlashIndex >= 0) {
            fileName = fileName.substring(lastSlashIndex + 1);
        }
        if (!StringUtils.hasText(fileName)) {
            return "file-" + System.currentTimeMillis();
        }
        return fileName;
    }

    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";
    }

    private boolean acquireLock(RLock lock, long waitTime, long leaseTime, String failMessage) {
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(failMessage, e);
        }
    }

    private Set<String> extractManagedImageUrls(String html, String bucket) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(html)) {
            return result;
        }

        Document document = Jsoup.parseBodyFragment(html);
        for (Element img : document.select("img[src]")) {
            String src = img.attr("src");
            if (isManagedMinioUrl(src, bucket)) {
                result.add(src);
            }
        }
        return result;
    }

    private boolean isManagedMinioUrl(String url, String bucket) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return url.contains("/" + bucket + "/");
    }

    private void scheduleImageDeleteAfterCommit(Set<String> imageUrls, String bucket) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Set<String> finalUrls = new LinkedHashSet<>(imageUrls);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteImagesFromMinio(finalUrls, bucket);
                }
            });
            return;
        }

        deleteImagesFromMinio(imageUrls, bucket);
    }

    private void deleteImagesFromMinio(Set<String> imageUrls, String bucket) {
        for (String imageUrl : imageUrls) {
            try {
                String objectName = extractObjectNameFromUrl(imageUrl, bucket);
                if (StringUtils.hasText(objectName)) {
                    minioUtil.removeObject(objectName, bucket);
                }
            } catch (Exception e) {
                log.warn("删除 MinIO 图片失败: {}", imageUrl, e);
            }
        }
    }

    private String extractObjectNameFromUrl(String fileUrl, String bucket) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            String prefix = "/" + bucket + "/";
            int index = path.indexOf(prefix);
            if (index == -1) {
                return null;
            }
            return path.substring(index + prefix.length());
        } catch (Exception e) {
            return null;
        }
    }
}
