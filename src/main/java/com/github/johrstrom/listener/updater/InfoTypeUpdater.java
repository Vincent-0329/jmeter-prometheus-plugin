package com.github.johrstrom.listener.updater;

import com.github.johrstrom.collector.SuccessRatioCollector;
import com.github.johrstrom.listener.ListenerCollectorConfig;
import com.googlecode.mp4parser.authoring.SampleImpl;
import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Info;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the AbstractUpdater sub-type that can handle updating any kind of Counter metrics
 * along with {@link SuccessRatioCollector} type.
 *
 * @author Jeff ohrstrom
 */
public class InfoTypeUpdater extends AbstractUpdater {
    private static AtomicInteger ai = new AtomicInteger(1);

    private static final Logger log = LoggerFactory.getLogger(InfoTypeUpdater.class);

    public InfoTypeUpdater(ListenerCollectorConfig cfg) {
        super(cfg);
    }

    @Override
    public void update(SampleEvent event) {
        try {
            if (this.config.listenToAssertions()) {
                for (AssertionResult assertion : event.getResult().getAssertionResults()) {
                    if (assertion.isFailure()) {
                        updateAssertions(new AssertionContext(assertion, event));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Did not update {} because of error: {}", this.config.getMetricName(), e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }

    protected void updateAssertions(AssertionContext ctx) {
        int a = ai.getAndAdd(1);
        String[] labels = this.labelValues(ctx);
        String message = ctx.event.getResult().getFirstAssertionFailureMessage();
        Collector collector = registry.getOrCreateAndRegister(this.config);
        Info info = (Info) collector;
        info.labels(labels).info(new String[]{"id",a+"","failuremessage", message});
    }
}
