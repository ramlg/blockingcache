# blockingcache

Ehcache BlockingCache when used along spring cache leaves other threads blocked until timeout or forever in case of no timeout after an exception is thrown by the cachable method.

The solution is to define an CacheInterceptor and put a null value for the key after the exception is thrown. 

FailSafeCacheInterceptor overrides the invoke method with one addition of unlocking the cache for the given key by putting a null value for the given key.

The issue while doing this was getting the key generated as CacheOperationContext has generateKey method defined as protected, which means this cannot be used directly to get the key.
FailSafteCacheOperationContext is defined to extend CacehOperationContext and have one extra method to return the key. 
This will make sure the the key is always same as the one used by BlockingCache.
