package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.dto.BookInfoDTO;
import com.etread.dto.BookSearchDTO;
import com.etread.dto.BookUploadDTO;
import com.etread.dto.UserDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import com.etread.entity.BookTag;
import com.etread.entity.BookTagRelation;
import com.etread.mapper.BookChapterMapper;
import com.etread.mapper.BookInfoMapper;
import com.etread.mapper.BookTagMapper;
import com.etread.mapper.BookTagRelationMapper;
import com.etread.service.BookChapterService;
import com.etread.service.BookInfoService;
import com.etread.utils.IdGenerator;
import com.etread.utils.MinioUtil;
import com.etread.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.annotations.Select;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookInfoServiceImpl extends ServiceImpl<BookInfoMapper, BookInfo> implements BookInfoService {
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private BookTagMapper bookTagMapper;

    @Autowired
    private BookTagRelationMapper bookTagRelationMapper;

    @Autowired
    private BookChapterService bookChapterService;
    @Autowired
    private BookChapterMapper bookChapterMapper;
    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Override
    public boolean updateStatus(BookInfoDTO bookInfo) {
        Long bookid = bookInfo.getBookid();
        int status = bookInfo.getStatus();
        String errorMsg = bookInfo.getError_msg();
        return this.lambdaUpdate()
                .eq(BookInfo::getId, bookid)
                .set(BookInfo::getStatus, status)
                .set(errorMsg != null, BookInfo::getErrorMsg, errorMsg)
                .update();
    }

    public BookInfoDTO buildBookInfoDTO(BookUploadDTO book, String token) {
        MultipartFile file = book.getFile();
        File localTempFile = null;
        Long bookid = IdGenerator.generateId();
        BookInfoDTO result = new BookInfoDTO();
        String fileUrl = null;
        String coverUrl = null;

        // 1. 验证 token 并设置 publisher
        try {
            String fullKey = "login:token:" + token;
            String userJson = redisUtil.get(fullKey);
            if (userJson != null) {
                UserDTO userDTO = JSON.parseObject(userJson, UserDTO.class);
                Long account = userDTO.getUser_id();
                book.setPublisher(account);
            } else {
                throw new RuntimeException("token 不存在或已过期");
            }
        } catch (Exception e) {
            throw new RuntimeException("上传者非法", e);
        }

        // 2. 上传主文件和封面
        try {
            fileUrl = minioUtil.uploadFile(file, "books");

            MultipartFile cover = book.getCover();
            if (cover != null) {
                byte[] bytes = cover.getBytes();
                String contentType = cover.getContentType();
                // ✅ 修复：使用封面的原始文件名，而不是主文件的
                String originalFilename = cover.getOriginalFilename();
                String objectName = "cover/" + System.currentTimeMillis() + "_" + originalFilename;
                coverUrl = minioUtil.uploadBytes(bytes, objectName, contentType, "contentpicture");
            } else {
                throw new RuntimeException("封面不能为空");
            }
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 3. 构建并保存数据库记录
        BookInfo bookInfo = transferdtotodao(book);
        bookInfo.setOriginalFileUrl(fileUrl);
        bookInfo.setCoverUrl(coverUrl);
        bookInfo.setId(bookid);
        bookInfo.setTotalScore(0);
        bookInfo.setRatingCount(0);
        this.save(bookInfo);
        saveTags(bookid, book.getTags());
        // 4. 准备返回结果
        result = transferdtotodto(book);
        result.setBookid(bookid);
        result.setCover_url(coverUrl);
        result.setFilename(file.getOriginalFilename());

        // 5. 下载临时文件（用于后续解析）
        try {
            // ✅ 修复：从完整 URL 中提取对象名称
            String objectName = extractObjectNameFromUrl(fileUrl, "books");
            localTempFile = minioUtil.downloadToTemp("books", objectName);
            result.setFile(localTempFile);

            // ⚠️ 标记 JVM 退出时删除临时文件（作为简单清理，生产环境建议由调用方管理）
            if (localTempFile != null) {
                localTempFile.deleteOnExit();
            }
        } catch (Exception e) {
            // 下载失败时，删除已保存的数据库记录（保持一致性）
            this.removeById(bookid);
            throw new RuntimeException("临时文件下载失败", e);
        }

        return result;
    }

    /**
     * 从 MinIO 文件 URL 中提取对象名称
     * @param fileUrl 完整 URL，格式：{endpoint}/{bucket}/{objectName}
     * @param bucket 存储桶名称（用于确认路径）
     * @return 对象名称
     */
    private String extractObjectNameFromUrl(String fileUrl, String bucket) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath(); // 例如：/books/folder/file.epub
            // 查找 "/bucket/" 之后的部分
            String prefix = "/" + bucket + "/";
            int index = path.indexOf(prefix);
            if (index != -1) {
                return path.substring(index + prefix.length());
            } else {
                // 如果路径不包含 bucket，直接返回整个路径（去掉开头的斜杠）
                return path.startsWith("/") ? path.substring(1) : path;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法从URL中提取对象名称: " + fileUrl, e);
        }
    }


    /**
     * 私有方法：保存标签并建立关联
     * 标签存储
     */
    private void saveTags(Long bookId, List<String> tags) {
        List<String> allowedTags = filterAllowedTags(tags);
        if (allowedTags.isEmpty()) return;

        LambdaQueryWrapper<BookTag> query = new LambdaQueryWrapper<>();
        query.in(BookTag::getTagName, allowedTags);
        List<BookTag> tagRows = bookTagMapper.selectList(query);
        Map<String, Long> tagNameToId = tagRows.stream()
                .collect(Collectors.toMap(BookTag::getTagName, BookTag::getId, (a, b) -> a));

        for (String tagName : allowedTags) {
            Long tagId = tagNameToId.get(tagName);
            if (tagId == null) {
                continue;
            }
            BookTagRelation relation = new BookTagRelation();
            relation.setBookId(bookId);
            relation.setTagId(tagId);
            bookTagRelationMapper.insert(relation);
        }
    }

    private List<String> filterAllowedTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> normalized = new ArrayList<>(new LinkedHashSet<>(
                tags.stream()
                        .filter(t -> t != null && !t.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.toList())
        ));

        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<BookTag> query = new LambdaQueryWrapper<>();
        query.in(BookTag::getTagName, normalized).select(BookTag::getTagName);
        List<BookTag> existRows = bookTagMapper.selectList(query);
        Set<String> existNames = existRows.stream()
                .map(BookTag::getTagName)
                .collect(Collectors.toSet());

        return normalized.stream()
                .filter(existNames::contains)
                .collect(Collectors.toList());
    }

    /**
     * 书籍基本信息修改
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBook(BookInfoDTO dto) {
        // 1. 更新基本信息
        BookInfo bookInfo = new BookInfo();
        bookInfo.setId(dto.getBookid());

        // 判空处理：只有当 DTO 中的字段不为 null 时，才设置到 Entity 中
        // 这样 MyBatis-Plus 在生成 Update SQL 时会自动忽略 null 字段，实现部分更新
        if (dto.getTitle() != null) {
            bookInfo.setTitle(dto.getTitle());
        }
        if (dto.getAuthor() != null) {
            bookInfo.setAuthor(dto.getAuthor());
        }
        if (dto.getDescription() != null) {
            bookInfo.setDescription(dto.getDescription());
        }

        // 更新冗余字段
        if (dto.getTags() != null) {
            List<String> allowedTags = filterAllowedTags(dto.getTags());
            bookInfo.setTags(String.join(",", allowedTags));
        }

        // 处理封面更新
        if(dto.getCover() != null){
            updateBookCover(dto.getBookid(), dto.getCover());

        }


        boolean updateResult = this.updateById(bookInfo);

        // 2. 更新标签关联（先删后加策略）
        if (updateResult && dto.getTags() != null) {
            // 删除旧关联
            LambdaQueryWrapper<BookTagRelation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookTagRelation::getBookId, dto.getBookid());
            bookTagRelationMapper.delete(wrapper);

            // 保存新关联
            saveTags(dto.getBookid(), dto.getTags());
        }

        return updateResult;
    }

    /**
     * 解决问题2：书籍删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBook(Long bookId) {
        // 0. 认证信息，获取书籍信息以便删除文件
        BookInfo bookInfo = this.getById(bookId);
        // 1. 删除 MinIO 中的资源
        try {
            // 删除原始文件 (books 桶)
            if (bookInfo.getOriginalFileUrl() != null) {
                String objectName = extractObjectNameFromUrl(bookInfo.getOriginalFileUrl(), "books");
                minioUtil.removeObject(objectName, "books");
            }
            // 删除封面 (contentpicture 桶)
            if (bookInfo.getCoverUrl() != null) {
                String objectName = extractObjectNameFromUrl(bookInfo.getCoverUrl(), "contentpicture");
                minioUtil.removeObject(objectName, "contentpicture");
            }
            // 删除章节图片 (contentpicture 桶，路径为 books/{bookId}/...)
            String objectname="books/" + bookInfo.getId()+"/";
            minioUtil.removeFolder("contentpicture", objectname);
            
        } catch (Exception e) {
            log.error("删除 MinIO 文件失败", e);
            // 不阻断数据库删除流程
        }

        // 2. 删除标签关联
        LambdaQueryWrapper<BookTagRelation> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(BookTagRelation::getBookId, bookId);
        bookTagRelationMapper.delete(tagWrapper);
//
//        // 3. 删除书籍章节（级联删除章节及内容）
//        bookChapterService.removeByBookId(bookId);

        // 3. 删除书籍基本信息
        return this.removeById(bookId);
    }

    public BookInfo transferdtotodao(BookUploadDTO book) {
        BookInfo bookInfo = new BookInfo();
        bookInfo.setTitle(book.getTitle());
        bookInfo.setAuthor(book.getAuthor());
        List<String> allowedTags = filterAllowedTags(book.getTags());
        bookInfo.setTags(String.join(",", allowedTags));
        bookInfo.setDescription(book.getDescription());
        bookInfo.setPublisher(book.getPublisher());
        bookInfo.setStatus(0);
        return bookInfo;
    }

    public BookInfoDTO transferdtotodto(BookUploadDTO book) {
        BookInfoDTO bookInfoDTO = new BookInfoDTO();
        bookInfoDTO.setTitle(book.getTitle());
        bookInfoDTO.setAuthor(book.getAuthor());
        bookInfoDTO.setTags(filterAllowedTags(book.getTags()));
        bookInfoDTO.setDescription(book.getDescription());
        bookInfoDTO.setStatus(0);
        return bookInfoDTO;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateBookCover(Long bookId, MultipartFile newCoverFile) {
        // 0. 基本检查：MultipartFile 用 isEmpty() 检查
        if (newCoverFile == null || newCoverFile.isEmpty()) {
            throw new RuntimeException("文件根本不存在或为空");
        }

        // 1. 先去数据库查出这本书
        BookInfo book = this.getById(bookId);
        if (book == null) {
            throw new RuntimeException("哎呀，找不到这本书！");
        }

        // 2. 🗑️ 找出旧衣服，把它扔掉！
        String oldUrl = book.getCoverUrl();
        if (oldUrl != null && !oldUrl.isEmpty()) {
            try {
                // 建议把这个 endpoint 提取到配置文件里 @Value 读取，不要写死
                String endpoint = "http://localhost:9000";
                String bucket = "contentpicture";

                // 如果是咱们 MinIO 的链接，才去删
                if (oldUrl.startsWith(endpoint + "/" + bucket)) {
                    // 抠出 objectName (去掉 http://.../bucket/)
                    String oldObjectName = oldUrl.substring((endpoint + "/" + bucket + "/").length());
                    minioUtil.removeObject(oldObjectName, bucket);
                }
            } catch (Exception e) {
                // 删旧图失败不影响换新图，记个日志就行
                log.warn("旧封面删除失败，可能是文件已经不存在了", e);
            }
        }

        // 3. 🆕 穿上新衣服！
        try {
            // 🌟 重点修正 1：获取文件名要用 getOriginalFilename()
            String originalName = newCoverFile.getOriginalFilename();
            if (originalName == null) originalName = "unknown.jpg"; // 防御性编程

            String suffix = "";
            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex >= 0) {
                suffix = originalName.substring(dotIndex);
            }
            String newObjectName = "cover/" + System.currentTimeMillis() + suffix;

            // 🌟 重点修正 2：获取类型直接用 getContentType()
            String contentType = newCoverFile.getContentType();
            if (contentType == null) {
                contentType = "image/jpeg"; // 默认兜底
            }

            // 🌟 重点修正 3：获取字节直接用 getBytes()
            byte[] fileBytes = newCoverFile.getBytes();

            // 上传
            String newUrl = minioUtil.uploadBytes(
                    fileBytes,
                    newObjectName,
                    contentType,
                    "contentpicture"
            );

            // 4. 📝 登记新户口
            book.setCoverUrl(newUrl);
            this.updateById(book);

            return true;

        } catch (Exception e) {
            throw new RuntimeException("换封面失败啦！", e);
        }
    }
    //word count
    @Override
    public long countwordbyBookId(Long bookId) {
        Long total = bookChapterMapper.sumWordCountByBookId(bookId);
        long wordCount = total == null ? 0L : total;
        this.lambdaUpdate()
                .eq(BookInfo::getId, bookId)
                .set(BookInfo::getWordCount, Math.toIntExact(wordCount))
                .update();
        return wordCount;
    }
    //书籍搜索方法实现
    @Override
    public Page<BookInfo> searchBook(BookSearchDTO bookSearchDTO, int page, int size){
        if (bookSearchDTO.getTags() != null && !bookSearchDTO.getTags().isEmpty()) {
            bookSearchDTO.setTagCount(bookSearchDTO.getTags().size());
        }
        Page<BookInfo> p = new Page<>(page, size);
        bookInfoMapper.searchBooks(p, bookSearchDTO);
        return p;
    }
    @Override
    public BookInfo getBookById(Long bookId){
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        return bookInfo;
    }
}
