package com.xh.business.ddd;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/6/6
 * @description 备注信息
 */
public class CompletableFutureUtils {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 单条异步执行
     *
     * @param recordList 数据行
     * @param function 转换函数
     */
    public <T, R> List<R> runSingleAsync(Collection<T> recordList, Function<T, CompletableFuture<R>> function) {
        List<CompletableFuture<R>> futures = recordList.stream()
                .map(function)
                .toList();

        // 等待所有请求完成
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // 合并结果，并处理每个 future 的异常情况
        return allDone.thenApply(v -> futures.stream()
                        .map(future -> {
                            try {
                                return future.get(); // 显式捕获异常
                            } catch (InterruptedException | ExecutionException e) {
                                Thread.currentThread().interrupt(); // 恢复中断状态
                                throw new RuntimeException("异步任务执行失败", e);
                            }
                        })
                        .collect(Collectors.toList()))
                .join();
    }

    /**
     * 单条异步执行
     *
     * @param recordList 数据行
     * @param function 转换函数
     */
    public <T, R> List<R> runMuliAsync(Collection<T> recordList, Function<T, CompletableFuture<R>> function) {
        List<CompletableFuture<R>> futures = recordList.stream()
                .map(function)
                .toList();

        // 等待所有请求完成
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // 合并结果，并处理每个 future 的异常情况
        return allDone.thenApply(v -> futures.stream()
                        .map(future -> {
                            try {
                                return future.get(); // 显式捕获异常
                            } catch (InterruptedException | ExecutionException e) {
                                Thread.currentThread().interrupt(); // 恢复中断状态
                                throw new RuntimeException("异步任务执行失败", e);
                            }
                        })
                        .collect(Collectors.toList()))
                .join();
    }


}
