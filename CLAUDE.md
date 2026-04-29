# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Spring Boot 3.1.5 / Spring Cloud 2022.0.4 / Spring Cloud Alibaba 2022.0.0.0-RC2, Java 17, Maven multi-module. MyBatis-Plus 3.5.3.1, jjwt 0.11.5, Lombok, fastjson2. The shell is `bash`, but the dev box is Windows — paths in `docker-compose.yml` and most yaml files use Windows-style `C:/Users/...`.

## Modules

Parent pom at the repo root. All children inherit the parent and pull `etread-common` as a dependency.

| Module | Port | Role |
|---|---|---|
| `etread-gateway` | 8722 | Spring Cloud Gateway (reactive). Auth, rate limit, blacklist, dynamic routes. |
| `etread-module-user` | 8081 | Login/register/logout. JWT + BCrypt. |
| `etread-module-book` | 8082 | Books, chapters, bookshelf, author writing, reviews, search, prewarm. |
| `etread-module-comment` | 8083 | Paragraph comments and likes with Redis Lua + scheduled flush. |
| `etread-common` | — | Shared `Result`, `JwtUtil`, `RedisUtil`, `MinioUtil`, `LoginInterceptor`, `GlobalWebConfig` (auto-registers the interceptor in any MVC module), `MybatisPlusConfig`, `GlobalExceptionHandler`. |

`etread-module-user` and `etread-module-book` have their own `mvnw` wrappers; the parent does not. Use `./etread-module-book/mvnw` or a globally installed `mvn`.

## Infrastructure (must be running locally)

- **Nacos** at `127.0.0.1:8048` — service discovery + config. Namespace `public`, group `DEFAULT_GROUP`. Gateway also pulls four shared configs: `gateway-routes.yaml`, `gateway-security.yaml`, `gateway-ratelimit.yaml`, `gateway-cors.yaml`. **Not** in `docker-compose.yml` — run separately.
- **MySQL** at `127.0.0.1:3306`, db `etread`, user `root`, password `liu123` (hardcoded in `application.yml`s). Schema lives at `etread-common/src/main/resources/etread.sql` — import it once.
- **Redis** at `127.0.0.1:6379` — started by `docker-compose.yml`. Both servlet modules and the reactive gateway connect.
- **MinIO** at `127.0.0.1:9000` (API) / `:9001` (console), root `minioadmin`/`minioadmin` — started by `docker-compose.yml`. Used for cover/file/image upload.

`docker compose up -d` brings up Redis + MinIO only. Nacos and MySQL are external.

## Common commands

```bash
# Build everything from the repo root
mvn clean install -DskipTests

# Build/run a single module (use a wrapper from a child module if needed)
mvn -pl etread-module-book -am clean package
mvn -pl etread-module-book spring-boot:run

# Tests
mvn test
mvn -pl etread-module-book test -Dtest=TxtBookParserTest
```

Boot order at runtime: Nacos + MySQL + Redis + MinIO → user → book → comment → gateway. Each Spring Boot app has its own `main` (e.g. `EtreadGatewayApplication`, `UserApplication`, `EtreadModuleBookApplication`, `EtreadModuleCommentApplication`).

## Architecture: how requests flow

1. Browser hits the gateway under `/api/{module}/...`. CORS allows `http://localhost:5173` / `127.0.0.1:5173` only.
2. Gateway resolves the route from `etread.gateway.dynamic.routes` in `etread-gateway/src/main/resources/application.yml` (also overridable from Nacos `gateway-routes.yaml`). The `RewritePath` filter strips `/api/{module}` so downstream modules see plain `/{module}/...`.
3. `GatewayAuthGlobalFilter` (order `-100`) reads the route metadata `authRequired`. If true, it pulls the token from header `token` or `Authorization: Bearer …`, looks up `login:token:{token}` in Redis via `TokenAuthService`, and **stamps `X-User-Id`, `X-Account`, `X-Nickname`, `X-Avatar`, plus the original `token` header onto the upstream request**. Missing/expired token → `401` with a Chinese message.
4. `GatewayRateLimitGlobalFilter` (order `-90`) applies a sliding-window limiter keyed by route metadata `rateLimitRule` (`login` / `bookRead` / `commentAction`). Strategies: `account_ip` (login), `user_uri` (authenticated routes), `ip_uri` (fallback).
5. The downstream MVC module receives the request. **Auth is double-checked** — `GlobalWebConfig` in `etread-common` registers `LoginInterceptor` on `/**` (excluding `/auth/login`, `/auth/register`, `/auth/logout`, swagger), which re-reads `login:token:{token}` from Redis and refreshes its 30-min TTL.
6. Controllers resolve the caller with `BookUserResolver` / `CommentUserResolver` / `Tokencheck`. These all read the same `login:token:{token}` JSON value (`UserDTO`) from Redis — the gateway-injected `X-User-Id` header is currently **not** used downstream.

The implication: if you change auth (token format, header name, Redis key, TTL), update **all four** of `GatewayAuthGlobalFilter`, `LoginInterceptor`, `*UserResolver`/`Tokencheck`, and `JwtUtil`/`AuthConstant`.

## Cross-cutting conventions

- **API envelope** is always `com.etread.Result<T>` — `code`/`msg`/`data`. Use `Result.success(msg, data)` / `Result.error(msg)`. The gateway emits its own `GatewayResult` with the same shape for 401/429/etc.
- **Controllers receive form-encoded bodies** even when the docs say JSON — DTOs are bound by Spring's default model-binder (no `@RequestBody` on most endpoints). Check the actual controller before assuming JSON.
- **Auth header on every authenticated endpoint is `@RequestHeader("token")`**, not `Authorization`. The gateway accepts both, but downstream code only reads `token`.
- **MyBatis-Plus pagination is enabled globally** via `MybatisPlusConfig` in `etread-common`. The comment in that file warns: without this interceptor, `Page<T>` queries would return all rows.
- **JWT signing key is hardcoded** in `etread-common/utils/JwtUtil.java` (`SECRET_STR`). Tokens are issued at login but the runtime auth check is **Redis lookup** — JWT parsing is rarely on the hot path.
- **`@MapperScan("com.etread.mapper")`** on each app's main class means all mappers must live under `com.etread.mapper.*` regardless of which module they're in.
- **`scanBasePackages = "com.etread"`** on each main class is intentional — it lets each module pick up beans from `etread-common` (interceptor, MinIO util, exception handler).

## Module-specific notes

### etread-gateway
Reactive (WebFlux). Excludes `DataSourceAutoConfiguration` so it never tries to connect to MySQL. Routes are loaded by `DynamicRouteDefinitionLocator` from `GatewayDynamicRouteProperties`; a Nacos config refresh triggers `GatewayRouteRefreshListener` to push new routes. Rate-limit Redis Lua and decision logic live in `ratelimit/SlidingWindowRateLimitService`.

### etread-module-book
- File parsing: `parser/BookParser` (interface) + `parser/impl/{TxtBookParser,EpubBookParser}`, picked by extension in `parser/factory/ParserFactory`. Uses jsoup + epublib-core.
- Heavy parsing runs on a custom `bookParseExecutor` thread pool defined in `EtreadModuleBookApplication` (core 4 / max 8 / queue 200, `CallerRunsPolicy`). `BookParseServiceImpl` uses `@Async("bookParseExecutor")`.
- `ChapterController.contents` triggers `ReadAheadPrewarmService.prewarmNextChaptersAsync` to warm the next 1–2 chapters in Redis (`book:chapter:content:{chapterId}`, 10-min TTL).
- `HotBookPrewarmScheduler` is currently fully commented out — the `book:hot` ZSet has no producer, so daily prewarm would no-op. See `learn/update-summary-2026-03-30.md` for the open todos.
- `Tokencheck.checkToken(token, bookId)` enforces "only the original `publisher` can delete/edit a book."
- Uses Redisson (`RedissonConfig`) for distributed locks and RoaringBitmap.

### etread-module-comment
- Likes are atomic Lua scripts in `CommentLikeServiceImpl` updating six Redis structures at once: liked-users `Set`, per-chapter like-count `Hash`, hot-comment `ZSet`, plus three "dirty" sets (`comment:like:changed:chapters`, `comment:like:changed:comments:{chapterId}`, `comment:like:pending:add|remove:{commentId}`). All key templates and the builder live in `constant/CommentRedisKeyConstants` + `utils/CommentRedisKeyBuilder` — never hand-format these.
- `CommentLikeFlushScheduler` runs `@Scheduled(fixedDelay = 10000)` and reconciles dirty-set state into `book_paragraph_comment.like_count` and `comment_like_record`.
- `CommentQueryServiceImpl` uses Redis pipeline for batch `SISMEMBER` + `Hash` multi-get when assembling the per-paragraph comment tree, then falls back to MySQL `like_count` if Redis is cold.
- New comments must be initialized in Redis at publish time (`CommentPublishServiceImpl`) so the like Lua scripts see consistent zero-state.

### etread-common
Putting beans here is the canonical way to share them — every module sets `scanBasePackages = "com.etread"`. Adding a new Spring bean here will be picked up by all MVC modules; for the gateway (reactive) you must check that no servlet-only auto-config gets dragged in.

## Reference

- `learn/comment-module-guide.md` — step-by-step rationale for the comment module's design (Redis key shape, why Lua, why dirty sets, etc.).
- `learn/cache-prewarm-guide.md` — prewarm strategy and key conventions.
- `learn/book-review-guide.md` — review/rating data model and transactional flow.
- `learn/update-summary-2026-03-30.md` — most recent change log + open design questions (e.g. missing `UNIQUE(book_id, user_id)` on `book_review`, missing `book:hot` producer).
