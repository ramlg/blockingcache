package com.ramlg.blockingcache;

import com.ramlg.config.CacheConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ramlalgoel on 16/06/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CacheConfig.class})
@ActiveProfiles("test")
public class FailSafeCacheInterceptorTest {

    @Autowired
    MockService service;

    @Autowired
    ThreadPoolTaskExecutor executor;

    @Autowired
    private EhCacheCacheManager cacheManager;

    @Before
    public void reset() throws Exception {
        waitForExecutor();
        Cache cache = cacheManager.getCache("cacheService");
        cache.clear();
        service.resetCounter();

    }

    @Test
    public void slowMethodIsCalledOnce() throws Exception {
        execute(threadSlowTask("A"), threadSlowTask("A"), threadSlowTask("A"));
        Assert.assertThat(service.getCount(), CoreMatchers.is(1));
    }

    @Test
    public void lockReleasedAfterException() throws Exception {

        execute(threadExceptionTask("A"), threadExceptionTask("A"));
        Assert.assertThat(service.getCount(), CoreMatchers.is(2));
    }

    @Test
    public void lockReleasedAfterExceptionWhereMethodNameIsKey() throws Exception {

        execute(threadMethodNameKey("A"), threadMethodNameKey("A"));
        Assert.assertThat(service.getCount(), CoreMatchers.is(2));
    }

    private Callable<Void> threadSlowTask(final String cacheKey) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.mockSlowMethod(cacheKey);
                return null;
            }
        };
    }

    private Callable<Void> threadExceptionTask(final String cacheKey) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.mockThrow(cacheKey);
                return null;
            }
        };

    }

    private Callable<Void> threadMethodNameKey(final String cacheKey) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.mockThrowMethodNameAsKey(cacheKey);
                return null;
            }
        };

    }
    private final void execute(Callable<Void>... tasks) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(tasks.length);
        executorService.invokeAll(Arrays.asList(tasks));
        executor.shutdown();
    }

    private void waitForExecutor() throws  InterruptedException {
        while(!executor.getThreadPoolExecutor().getQueue().isEmpty() || executor.getActiveCount() > 0) {
            Thread.sleep(50);
        }
    }

    @Service
    public static class MockService {

        private final AtomicInteger counter = new AtomicInteger();

        @Cacheable("cacheService")
        public Integer mockSlowMethod(String cacheKey) throws InterruptedException {
            Thread.sleep(1000);
            return counter.incrementAndGet();
        }

        @Cacheable("cacheService")
        public Integer mockThrow(String cacheKey) throws InterruptedException {
            counter.incrementAndGet();
            Thread.sleep(500);
            throw new RuntimeException();
        }

        @Cacheable(value = "cacheService", key = "#root.methodName")
        public Integer mockThrowMethodNameAsKey(String cacheKey) throws InterruptedException {
            Thread.sleep(500);
            counter.incrementAndGet();
            throw new RuntimeException();
        }

        public void resetCounter() {
            counter.set(0);
        }

        public int getCount() {
            return counter.get();
        }
    }
}