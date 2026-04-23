package com.etread;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
@SpringBootApplication(scanBasePackages = "com.etread") // 1. 扩大扫描范围，能抓到 common 里的 MinioUtil
@MapperScan("com.etread.mapper")
@EnableScheduling
@EnableDiscoveryClient
public class EtreadModuleBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtreadModuleBookApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Etread 书籍模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }

    /**
     * 配置书籍并发解析线程池
     * 对应 BookParseServiceImpl 中的 @Qualifier("bookParseExecutor")
     */
    @Bean("bookParseExecutor")
    public Executor bookParseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据 CPU 核心数调整，解析是 CPU 密集型 + IO 密集型
        executor.setCorePoolSize(4);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量：缓冲等待的任务
        executor.setQueueCapacity(200);
        // 线程活跃时间 (秒)
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀
        executor.setThreadNamePrefix("BookParse-");
        // 拒绝策略：调用者运行 (防止任务丢失，但会阻塞主线程)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
