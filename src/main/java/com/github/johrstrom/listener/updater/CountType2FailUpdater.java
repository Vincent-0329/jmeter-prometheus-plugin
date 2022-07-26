package com.github.johrstrom.listener.updater;

import com.github.johrstrom.collector.SuccessRatioCollector;
import com.github.johrstrom.listener.ListenerCollectorConfig;
import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the AbstractUpdater sub-type that handle the failure threads metrics
 *
 * @author Jeff ohrstrom
 */
public class CountType2FailUpdater extends AbstractUpdater {

    private static final Logger log = LoggerFactory.getLogger(CountType2FailUpdater.class);

    public CountType2FailUpdater(ListenerCollectorConfig cfg) {
        super(cfg);
    }

    @Override
    public void update(SampleEvent event) {
        if (this.config.listenToSamples()) {
            boolean successful = event.getResult().isSuccessful();
            if(!successful){
                updateAssertions(event);
            }
        }
    }


    protected void updateAssertions(SampleEvent event) {
        try {
            String[] labels = this.labelValues(event);
            Collector collector = registry.getOrCreateAndRegister(this.config);

            Counter c = (Counter) collector;
            c.labels(labels).inc();

        } catch (Exception e) {
            log.error("Did not update {} because of error: {}", this.config.getMetricName(), e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }

}
