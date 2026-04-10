package com.etread.constant;

public final class CommentRedisKeyConstants {

    private CommentRedisKeyConstants() {
    }

    /**
     * 记录某条评论被哪些用户点赞过。
     * Key: comment:liked_users:{commentId}
     * Type: Set
     */
    public static final String COMMENT_LIKED_USERS_KEY = "comment:liked_users:%s";

    /**
     * 记录某一章节下各评论的点赞总数。
     * Key: comment:like_count:{chapterId}
     * Field: commentId
     * Value: likeCount
     * Type: Hash
     */
    public static final String COMMENT_LIKE_COUNT_KEY = "comment:like_count:%s";

    /**
     * 记录某一章节的热评榜。
     * Key: comment:hot:{chapterId}
     * Member: commentId
     * Score: likeCount
     * Type: ZSet
     */
    public static final String COMMENT_HOT_KEY = "comment:hot:%s";

    /**
     * 记录哪些章节存在待刷盘的点赞变更。
     * Key: comment:like:changed:chapters
     * Type: Set
     */
    public static final String COMMENT_LIKE_CHANGED_CHAPTERS_KEY = "comment:like:changed:chapters";

    /**
     * 记录某一章节下哪些评论存在待刷盘变更。
     * Key: comment:like:changed:comments:{chapterId}
     * Type: Set
     */
    public static final String COMMENT_LIKE_CHANGED_COMMENTS_KEY = "comment:like:changed:comments:%s";

    /**
     * 记录某条评论待新增到 MySQL 的点赞关系。
     * Key: comment:like:pending:add:{commentId}
     * Type: Set
     */
    public static final String COMMENT_LIKE_PENDING_ADD_KEY = "comment:like:pending:add:%s";

    /**
     * 记录某条评论待从 MySQL 删除的点赞关系。
     * Key: comment:like:pending:remove:{commentId}
     * Type: Set
     */
    public static final String COMMENT_LIKE_PENDING_REMOVE_KEY = "comment:like:pending:remove:%s";

    /**
     * 标记某条评论的点赞缓存已完成初始化。
     * Key: comment:like:cache:loaded:{commentId}
     * Type: String
     */
    public static final String COMMENT_LIKE_CACHE_LOADED_KEY = "comment:like:cache:loaded:%s";

    /**
     * 初始化某条评论点赞缓存时使用的短时锁。
     * Key: comment:like:cache:init:lock:{commentId}
     * Type: String
     */
    public static final String COMMENT_LIKE_CACHE_INIT_LOCK_KEY = "comment:like:cache:init:lock:%s";

    /**
     * 标记某一章节的热评榜缓存已完成初始化。
     * Key: comment:hot:cache:loaded:{chapterId}
     * Type: String
     */
    public static final String COMMENT_HOT_CACHE_LOADED_KEY = "comment:hot:cache:loaded:%s";

    /**
     * 初始化某一章节热评榜时使用的短时锁。
     * Key: comment:hot:cache:init:lock:{chapterId}
     * Type: String
     */
    public static final String COMMENT_HOT_CACHE_INIT_LOCK_KEY = "comment:hot:cache:init:lock:%s";
}
