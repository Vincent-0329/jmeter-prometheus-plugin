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
 * This is the AbstractUpdater sub-type that can handle updating any kind of Counter metrics
 * along with {@link SuccessRatioCollector} type.
 *
 * @author Jeff ohrstrom
 */
public class CountType2AssertFailUpdater extends AbstractUpdater {

    private static final Logger log = LoggerFactory.getLogger(CountType2AssertFailUpdater.class);

    public CountType2AssertFailUpdater(ListenerCollectorConfig cfg) {
        super(cfg);
    }

    @Override
    public void update(SampleEvent event) {
        if (this.config.listenToAssertions()) {
            for (AssertionResult assertion : event.getResult().getAssertionResults()) {
                updateAssertions(new AssertionContext(assertion, event));
            }
        }
    }


    protected void updateAssertions(AssertionContext ctx) {
        try {
            if (ctx.assertion.isFailure()) {

                String[] labels = this.labelValues(ctx);
                Collector collector = registry.getOrCreateAndRegister(this.config);


                Counter c = (Counter) collector;
                c.labels(labels).inc();
            }

        } catch (Exception e) {
            log.error("Did not update {} because of error: {}", this.config.getMetricName(), e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }

}
