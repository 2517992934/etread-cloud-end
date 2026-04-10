package com.etread.utils;

import com.etread.constant.CommentRedisKeyConstants;

public final class CommentRedisKeyBuilder {

    private CommentRedisKeyBuilder() {
    }

    public static String likedUsersKey(Long commentId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKED_USERS_KEY, commentId);
    }

    public static String likeCountKey(Long chapterId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_COUNT_KEY, chapterId);
    }

    public static String hotKey(Long chapterId) {
        return String.format(CommentRedisKeyConstants.COMMENT_HOT_KEY, chapterId);
    }

    public static String changedCommentsKey(Long chapterId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_CHANGED_COMMENTS_KEY, chapterId);
    }

    public static String pendingAddKey(Long commentId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_PENDING_ADD_KEY, commentId);
    }

    public static String pendingRemoveKey(Long commentId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_PENDING_REMOVE_KEY, commentId);
    }

    public static String likeCacheLoadedKey(Long commentId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_CACHE_LOADED_KEY, commentId);
    }

    public static String likeCacheInitLockKey(Long commentId) {
        return String.format(CommentRedisKeyConstants.COMMENT_LIKE_CACHE_INIT_LOCK_KEY, commentId);
    }

    public static String hotCacheLoadedKey(Long chapterId) {
        return String.format(CommentRedisKeyConstants.COMMENT_HOT_CACHE_LOADED_KEY, chapterId);
    }

    public static String hotCacheInitLockKey(Long chapterId) {
        return String.format(CommentRedisKeyConstants.COMMENT_HOT_CACHE_INIT_LOCK_KEY, chapterId);
    }
}
