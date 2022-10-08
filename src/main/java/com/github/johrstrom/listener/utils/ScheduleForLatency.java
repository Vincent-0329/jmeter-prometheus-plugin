package com.github.johrstrom.listener.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ScheduleForLatency {

    private static ConcurrentHashMap<String,Long[]> ScheduleLatency = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String,Long> AVGLatency= new ConcurrentHashMap<>();

    private static Logger log = LoggerFactory.getLogger(ScheduleForLatency.class);

    public ScheduleForLatency() {
    }

    public static ConcurrentHashMap<String, Long[]> getScheduleLatency() {
        return ScheduleLatency;
    }

    public static ConcurrentHashMap<String, Long> getAVGLatency() {
        return AVGLatency;
    }

    public static void LatencySehedule() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 1000);
                    ScheduleLatency.forEach((key,value)-> {
                        long latency = value[0];
                        long num = value[1];
                        long avg;
                        if (num > 0) {
                            avg = latency / num;
                        } else {
                            avg = 0;
                        }
                        AVGLatency.put(key, avg);
                        Long[] values = {0l, 0l};
                        ScheduleLatency.put(key, values);
                    });

                    log.debug("latency --- every 5 secends: {} ");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
