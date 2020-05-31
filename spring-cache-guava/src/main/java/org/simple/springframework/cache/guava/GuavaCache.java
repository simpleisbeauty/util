package org.simple.springframework.cache.guava;
/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class GuavaCache extends AbstractValueAdaptingCache {

    private final String name;

    private final com.google.common.cache.Cache<Object, Object> cache;


    /**
     * Create a {@link GuavaCache} instance with the specified name and the
     * given internal {@link com.google.common.cache.Cache} to use.
     * @param name the name of the cache
     * @param cache the backing Caffeine Cache instance
     */
    public GuavaCache(String name, com.google.common.cache.Cache<Object, Object> cache) {
        this(name, cache, true);
    }

    /**
     * Create a {@link GuavaCache} instance with the specified name and the
     * given internal {@link com.google.common.cache.Cache} to use.
     * @param name the name of the cache
     * @param cache the backing Caffeine Cache instance
     * @param allowNullValues whether to accept and convert {@code null}
     * values for this cache
     */
    public GuavaCache(String name, com.google.common.cache.Cache<Object, Object> cache,
                      boolean allowNullValues) {

        super(allowNullValues);
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(cache, "Cache must not be null");
        this.name = name;
        this.cache = cache;
    }


    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final com.google.common.cache.Cache<Object, Object> getNativeCache() {
        return this.cache;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        if (this.cache instanceof LoadingCache) {
            try {
                Object value = ((LoadingCache<Object, Object>) this.cache).get(key);
                return toValueWrapper(value);
            }
            catch (ExecutionException ex) {
                throw new UncheckedExecutionException(ex.getMessage()+ ": "+ key, ex);
            }
        }
        return toValueWrapper(this.cache.getIfPresent(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T get(Object key, final Callable<T> valueLoader) {
        try {
            return (T) fromStoreValue(this.cache.get(key, valueLoader));
        }
        catch (ExecutionException ex) {
            throw new UncheckedExecutionException(ex.getMessage()+ ": "+ key, ex);
        }
    }

    @Override
    @Nullable
    protected Object lookup(Object key) {
        return this.cache.getIfPresent(key);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        this.cache.put(key, toStoreValue(value));
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
        try {
            GuavaCache.PutIfAbsentCallable callable = new GuavaCache.PutIfAbsentCallable(value);
            Object result = this.cache.get(key, callable);
            return (callable.called ? null : toValueWrapper(result));
        }
        catch (ExecutionException ex) {
            throw new UncheckedExecutionException(ex.getMessage()+ ": "+ key, ex);
        }
    }

    @Override
    public void evict(Object key) {
        this.cache.invalidate(key);
    }

    @Override
    public void clear() {
        this.cache.invalidateAll();
    }

    /**
     * Private Callable wrapper with called status for PutIfAbsent
    * */
    private class PutIfAbsentCallable implements Callable<Object> {

        private final Object value;

        private boolean called;

        public PutIfAbsentCallable(Object value) {
            this.value = value;
        }

        @Override
        public Object call() throws Exception {
            this.called = true;
            return toStoreValue(this.value);
        }
    }


}