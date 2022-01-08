/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.netflix.eureka.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for getting a count in last X milliseconds.
 * 这个类挺好玩的，是个技术亮点，挺不错的，可以借鉴
 * @author Karthik Ranganathan,Greg Kim
 */
public class MeasuredRate {
    private static final Logger logger = LoggerFactory.getLogger(MeasuredRate.class);
    private final AtomicLong lastBucket = new AtomicLong(0);
    private final AtomicLong currentBucket = new AtomicLong(0);

    private final long sampleInterval;
    private final Timer timer;

    private volatile boolean isActive;

    /**
     * @param sampleInterval in milliseconds
     *                       sampleInterval 每分钟执行一次
     */
    public MeasuredRate(long sampleInterval) {
        this.sampleInterval = sampleInterval;
        this.timer = new Timer("Eureka-MeasureRateTimer", true);
        this.isActive = false;
    }

    public synchronized void start() {
        if (!isActive) {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    try {
                        // Zero out the current bucket.
                        // 每分钟调度一次
                        // 将当前的这个 88次，设置到 lastBucket中去
                        // 再将 currentBucket清零

                        // 举例说明一下
                        // currentBucket是用来更新当前这分钟的心跳次数的
                        // lastBucket是保留了上一分钟的心跳次数的
                        // 这个任务 一分钟执行一次，每次就将上一次的心跳次数设置到lastBucket中去
                        // 然后将currentBucket的次数清零，重新计算当前这分钟的心跳次数
                        lastBucket.set(currentBucket.getAndSet(0));
                    } catch (Throwable e) {
                        logger.error("Cannot reset the Measured Rate", e);
                    }
                }
            }, sampleInterval, sampleInterval);

            isActive = true;
        }
    }

    public synchronized void stop() {
        if (isActive) {
            timer.cancel();
            isActive = false;
        }
    }

    /**
     * Returns the count in the last sample interval.
     */
    public long getCount() {
        return lastBucket.get();
    }

    /**
     * Increments the count in the current sample interval.
     */
    public void increment() {
        // 这里的意思就是每来一次心跳就 + 1，但不能无限次的 加。
        // 比如一个服务实例 一分钟 加了 88次。然后上面有个定时任务会执行的
        currentBucket.incrementAndGet();
    }
}
