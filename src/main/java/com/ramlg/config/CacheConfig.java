package com.ramlg.config;

import com.ramlg.blockingcache.FailSafeCacheInterceptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.Executor;

/**
 * Created by ramlalgoel on 16/06/2017.
 */
@Configuration
@EnableAspectJAutoProxy
@EnableCaching(order = 1)
@EnableAsync(order = 2)
@ComponentScan("com.ramlg.blockingcache")
@DirtiesContext
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactoryBean() {
        EhCacheManagerFactoryBean ecmfb = new EhCacheManagerFactoryBean();
        ecmfb.setShared(true);
        ecmfb.setAcceptExisting(true);
        ecmfb.setCacheManagerName("testCacheMgr");
        ecmfb.setConfigLocation(new ClassPathResource("ehcache.xml"));
        return ecmfb;
    }

    @Bean
    public CacheManager cacheManager() {
        EhCacheCacheManager cacheManager = new EhCacheCacheManager();
        cacheManager.setCacheManager(ehCacheManagerFactoryBean().getObject());
        return cacheManager;
    }

    @Bean
    public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
        CacheInterceptor cacheInterceptor = new FailSafeCacheInterceptor();
        cacheInterceptor.setCacheManager(cacheManager());
        cacheInterceptor.setCacheOperationSources(cacheOperationSource);

        return cacheInterceptor;
    }

    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        return executor;
    }
}
