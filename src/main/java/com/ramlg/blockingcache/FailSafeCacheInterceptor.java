package com.ramlg.blockingcache;

import net.sf.ehcache.constructs.blocking.BlockingCache;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheableOperation;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by ramlalgoel on 16/06/2017.
 */
public class FailSafeCacheInterceptor  extends CacheInterceptor {

    /**
     * This method would is actually the same implementation as the super method with
     * extra call to unlock cache in case of an exception during method invokation.
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method =invocation.getMethod();
        CacheOperationInvoker invoker = invoker(invocation);
        try {
            return execute(invoker, invocation.getThis(), method, invocation.getArguments());
        }catch (CacheOperationInvoker.ThrowableWrapper th) {
            unblockCacheForException(invocation);
            throw th.getOriginal();
        }
    }

    @Override
    protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new FailSafeCacheOperationContext(metadata,args, target);
    }

    private CacheOperationInvoker invoker(final MethodInvocation invocation) {
        return () -> {
            try {
                return invocation.proceed();
            }catch (Throwable t) {
                throw new CacheOperationInvoker.ThrowableWrapper(t);
            }
        };
    }

    /**
     * This method gets all the caches for the given invocation and puts a null value for the given key.
     * which will in turn release the lock.
     * @param invocation
     */
    private void unblockCacheForException(MethodInvocation invocation) {

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(invocation.getThis());

        Optional<CacheOperation> operation = getCacheOperationSource()
                .getCacheOperations(invocation.getMethod(), targetClass)
                .stream()
                .filter(o -> o instanceof CacheableOperation)
                .findFirst();

        operation.ifPresent( op -> {
            FailSafeCacheOperationContext opContext = (FailSafeCacheOperationContext)getOperationContext(
                    op, invocation.getMethod(), invocation.getArguments(), invocation.getThis(), targetClass);

            Object key = opContext.getKey();

            getCacheResolver().resolveCaches(opContext)
                    .stream()
                    .filter(cache -> cache.getNativeCache() instanceof BlockingCache)
                    .forEach(cache -> cache.put(key, null));
        });
    }

    /**
     * This class is defined to get the key as generateKey method is protected.
     * And to do a put on cache we need the same key used by the BlockingCache
     */
    protected class FailSafeCacheOperationContext extends CacheOperationContext {

        public FailSafeCacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            super(metadata, args, target);
        }

        public Object getKey() {
            return generateKey(new Object());
        }
    }
}
