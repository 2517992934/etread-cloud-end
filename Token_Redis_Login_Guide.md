# Token + Redis 登录注册改造指南

> **写在前面：关于技术选型的思考**
>
> 你提到了两个非常好的问题：
> 1. **为什么不用 Spring Security？**
>    Spring Security 是业界标准，功能极其强大（防 CSRF、OAuth2、RBAC 等），但它的学习曲线非常陡峭，本质是一条复杂的过滤器链。对于新手来说，直接上手 Spring Security 往往会陷入“配置地狱”，而忽略了认证的本质。
>    **手写拦截器 + Redis** 方案更加轻量、透明。你写的每一行代码你都清楚在做什么（如何解析 Token、如何查 Redis、如何放行），这对理解“认证原理”至关重要。等你熟悉了这套机制，再升级到 Spring Security 会事半功倍。
> 2. **拦截器放在 `etread-module-user` 合适吗？**
>    你的直觉非常敏锐！如果放在 `user` 模块，它只能拦截 `user` 模块的请求。如果以后有了 `order`（订单）模块，岂不是要再写一遍？
>    **最佳实践**是将其**下沉到公共模块 (`etread-common`)**，这样任何引用了 `common` 的微服务都能自动拥有鉴权能力。
>
> **本指南将教你按照“最佳实践”的架构，将拦截器实现到 `etread-common` 中。**

## 1. 环境与依赖准备

### 1.1 添加 Redis 依赖

建议在 `etread-common/pom.xml` 中添加依赖，这样所有模块都能使用 Redis 能力。

**文件路径**: `etread-common/pom.xml`

```xml
<dependencies>
    <!-- Web 依赖（拦截器需要） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Redis 依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 对象池依赖 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
    
    <!-- FastJSON2 (建议使用最新版) -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.43</version>
    </dependency>
</dependencies>
```

### 1.2 配置 Redis

在启动类所在的模块（这里是 `etread-module-user`）中配置 Redis 连接。

**文件路径**: `etread-module-user/src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
```

---

## 2. 公共模块实现 (etread-common)

我们将核心鉴权逻辑全部封装在 `etread-common` 中。

### 2.1 Redis 工具类

**文件路径**: `etread-common/src/main/java/com/etread/utils/RedisUtil.java`

```java
package com.etread.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }
    
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }
}
```

### 2.2 定义常量

**文件路径**: `etread-common/src/main/java/com/etread/constant/AuthConstant.java`

```java
package com.etread.constant;

public class AuthConstant {
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";
    public static final long LOGIN_TOKEN_TTL = 30; // 30分钟
}
```

### 2.3 全局拦截器 (核心)

**文件路径**: `etread-common/src/main/java/com/etread/interceptor/LoginInterceptor.java`

```java
package com.etread.interceptor;

import com.etread.constant.AuthConstant;
import com.etread.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.concurrent.TimeUnit;

// 注意：这里不加 @Component，我们通过配置类统一管理
public class LoginInterceptor implements HandlerInterceptor {

    private RedisUtil redisUtil;

    public LoginInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Token
        String token = request.getHeader("Authorization");
        
        // 2. 判空
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            return false;
        }

        // 3. 查 Redis
        String key = AuthConstant.LOGIN_TOKEN_PREFIX + token;
        if (!redisUtil.hasKey(key)) {
            response.setStatus(401);
            return false;
        }

        // 4. 续期
        redisUtil.expire(key, AuthConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return true;
    }
}
```

### 2.4 自动配置类 (关键)

我们需要在 `common` 模块中写一个配置类，自动把拦截器注册到 Spring MVC 中。这样其他模块只要引入了 `common`，就会自动生效。

**文件路径**: `etread-common/src/main/java/com/etread/config/GlobalWebConfig.java`

```java
package com.etread.config;

import com.etread.interceptor.LoginInterceptor;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GlobalWebConfig implements WebMvcConfigurer {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(redisUtil))
                .addPathPatterns("/**") // 拦截所有
                // 这里配置通用的放行路径
                .excludePathPatterns(
                        "/auth/login", 
                        "/auth/register", 
                        "/doc.html", 
                        "/webjars/**", 
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                );
    }
}
```

---

## 3. 业务模块实现 (etread-module-user)

现在回到业务模块，我们只需要关注业务逻辑。

### 3.1 登录逻辑实现

**User 服务实现**: `etread-module-user/src/main/java/com/etread/service/impl/UserServiceImpl.java`

```java
@Autowired
private RedisUtil redisUtil; // 直接注入 Common 中的工具类

@Override
public String login(String account, String password) {
    // 1. 查用户
    User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
    if (user == null) throw new RuntimeException("账号不存在");

    // 2. 验密码
    if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("密码错误");
    }

    // 3. 生成 Token
    String token = UUID.randomUUID().toString().replace("-", "");

    // 4. 存 Redis
    String key = AuthConstant.LOGIN_TOKEN_PREFIX + token;
    user.setPassword(null); // 脱敏
    redisUtil.set(key, JSON.toJSONString(user), AuthConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

    return token;
}
```

---

## 4. 附录：关于 SecurityConfig.java

你提到的 `SecurityConfig.java` 文件目前只负责提供 `BCryptPasswordEncoder` 这个 Bean（用于密码加密），这和我们的拦截器方案**不冲突**。

如果你一定要用 **Spring Security** 来做鉴权（替代上述拦截器方案），你需要把 `SecurityConfig.java` 改成下面这样（仅供参考，不建议新手直接使用）：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 关闭 CSRF
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll() // 放行登录注册
                .anyRequest().authenticated() // 其他需要认证
            )
            // 这里需要你自己写一个 JwtAuthenticationTokenFilter 并加进去
            .addFilterBefore(new JwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}
```
**对比结论**：Spring Security 的核心也是 Filter，但它封装了很多层。为了学习，我们强烈建议你先按照**第 2 节**的方法，手动实现拦截器，彻底搞懂它。
