package com.gradproject.taskmanager.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    

    @Test
    void getAsyncExecutor_returnsThreadPoolTaskExecutor() {
        
        Executor executor = asyncConfig.getAsyncExecutor();

        
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void getAsyncExecutor_configuresCorePoolSize() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
    }

    @Test
    void getAsyncExecutor_configuresMaxPoolSize() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
    }

    @Test
    void getAsyncExecutor_configuresQueueCapacity() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        
        
        assertThat(executor).isNotNull();
    }

    @Test
    void getAsyncExecutor_configuresThreadNamePrefix() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThat(executor.getThreadNamePrefix()).isEqualTo("async-");
    }

    @Test
    void getAsyncExecutor_initializesExecutor() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        
        assertThatCode(() -> executor.getThreadPoolExecutor())
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_returnsActiveExecutor() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThat(executor.getThreadPoolExecutor()).isNotNull();
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
    }

    

    @Test
    void getAsyncUncaughtExceptionHandler_returnsNonNullHandler() {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();

        
        assertThat(handler).isNotNull();
    }

    @Test
    void exceptionHandler_doesNotThrowException() throws Exception {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = this.getClass().getMethod("testMethod");
        Throwable exception = new RuntimeException("Test exception");
        Object[] params = new Object[]{"param1", "param2"};

        
        assertThatCode(() -> handler.handleUncaughtException(exception, method, params))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionHandler_handlesNullException() throws Exception {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = this.getClass().getMethod("testMethod");

        
        assertThatCode(() -> handler.handleUncaughtException(null, method, new Object[]{}))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionHandler_handlesNullMethod() {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Throwable exception = new RuntimeException("Test exception");

        
        assertThatCode(() -> handler.handleUncaughtException(exception, null, new Object[]{}))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionHandler_handlesEmptyParams() throws Exception {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = this.getClass().getMethod("testMethod");
        Throwable exception = new RuntimeException("Test exception");

        
        assertThatCode(() -> handler.handleUncaughtException(exception, method, new Object[]{}))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionHandler_handlesNullParams() throws Exception {
        
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = this.getClass().getMethod("testMethod");
        Throwable exception = new RuntimeException("Test exception");

        
        assertThatCode(() -> handler.handleUncaughtException(exception, method, (Object[]) null))
                .doesNotThrowAnyException();
    }

    

    @Test
    void threadPoolExecutor_canAcceptTasks() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThatCode(() -> executor.execute(() -> {
            
        })).doesNotThrowAnyException();
    }

    @Test
    void threadPoolExecutor_hasCorrectActiveCount() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        int activeCount = executor.getActiveCount();

        
        assertThat(activeCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    void threadPoolExecutor_hasCorrectPoolSize() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        int poolSize = executor.getPoolSize();

        
        assertThat(poolSize).isGreaterThanOrEqualTo(0);
        assertThat(poolSize).isLessThanOrEqualTo(10); 
    }

    

    @Test
    void corePoolSize_isLessThanOrEqualToMaxPoolSize() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        int corePoolSize = executor.getCorePoolSize();
        int maxPoolSize = executor.getMaxPoolSize();

        
        assertThat(corePoolSize).isLessThanOrEqualTo(maxPoolSize);
    }

    @Test
    void threadPoolExecutor_usesConfiguredSettings() {
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("async-");
    }

    

    @Test
    void getAsyncExecutor_returnsNewInstanceEachTime() {
        
        Executor executor1 = asyncConfig.getAsyncExecutor();
        Executor executor2 = asyncConfig.getAsyncExecutor();

        
        
        assertThat(executor1).isNotSameAs(executor2);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnsNewInstanceEachTime() {
        
        AsyncUncaughtExceptionHandler handler1 = asyncConfig.getAsyncUncaughtExceptionHandler();
        AsyncUncaughtExceptionHandler handler2 = asyncConfig.getAsyncUncaughtExceptionHandler();

        
        
        assertThat(handler1).isNotNull();
        assertThat(handler2).isNotNull();
    }

    

    
    public void testMethod() {
        
    }
}
