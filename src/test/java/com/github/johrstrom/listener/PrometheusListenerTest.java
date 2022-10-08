package com.github.johrstrom.listener;

import com.github.johrstrom.collector.BaseCollectorConfig;
import com.github.johrstrom.collector.JMeterCollectorRegistry;
import com.github.johrstrom.collector.SuccessRatioCollector;
import com.github.johrstrom.test.TestUtilities;
import com.github.johrstrom.test.TestUtilities.ResultAndVariables;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Counter;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PrometheusListenerTest {

    private static final JMeterCollectorRegistry reg = JMeterCollectorRegistry.getInstance();

    static {
        TestUtilities.createJmeterEnv();
    }

    @Test
    public void listenerIsSerializable() throws IOException {
        ByteArrayOutputStream objectBuffer = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(objectBuffer);

        PrometheusListener listener = new PrometheusListener();
        out.writeObject(listener);

        Assert.assertNotNull(listener);
        Assert.assertTrue(objectBuffer.size() > 0);

    }

    @Test
    public void canHaveDuplicateMetrics() {
        PrometheusListener first = new PrometheusListener();
        PrometheusListener second = new PrometheusListener();

        first.setCollectorConfigs(TestUtilities.simpleListListener());
        second.setCollectorConfigs(TestUtilities.simpleListListener());

        first = (PrometheusListener) first.clone();
        second = (PrometheusListener) second.clone();

        first.testStarted();
        second.testStarted();

        first.testEnded();
        second.testEnded();

    }

    @Test
    public void lazyInizializationPrometheusServer() throws NoSuchFieldException, IllegalAccessException {

        PrometheusListener listener = new PrometheusListener();
        // Remove the server to test lazy initialization on test start
        Field prometheusServerField = listener.getClass().getDeclaredField("server");
        prometheusServerField.setAccessible(true);
        prometheusServerField.set(listener, null);
        listener.setCollectorConfigs(TestUtilities.simpleListListener());
        listener.testStarted();
        Assert.assertNotNull(prometheusServerField.get(listener));
    }

    @Test
    public void lazyInizializationRegistry() throws NoSuchFieldException, IllegalAccessException {

        PrometheusListener listener = new PrometheusListener();
        // Remove the server to test lazy initialization on test start
        Field registryField = listener.getClass().getSuperclass().getDeclaredField("registry");
        registryField.setAccessible(true);
        registryField.set(listener, null);
        listener.setCollectorConfigs(TestUtilities.simpleListListener());
        listener.testStarted();
        Assert.assertNotNull(registryField.get(listener));
    }

    public void canReadJMX() throws IOException {
        File jmx = new File("target/test-classes/simple_prometheus_example.jmx");
        HashTree tree = SaveService.loadTree(jmx);

        Assert.assertNotNull(tree);
    }

    @Test
    public void testLabelfailure() throws InterruptedException {

        PrometheusListener listener = new PrometheusListener();

        BaseCollectorConfig cfg = TestUtilities.simpleAsseFailCfg();
        cfg.setLabels(TestUtilities.TEST_LABELS_2);

        ListenerCollectorConfig lc = new ListenerCollectorConfig(cfg);
        lc.setMeasuring(ListenerCollectorConfig.Measurable.failureMessageAssert.toString());
        lc.setMetricName("test_info_failuremessage");
        lc.setListenTo(ListenerCollectorConfig.ASSERTIONS);

        List<ListenerCollectorConfig> list = new ArrayList<ListenerCollectorConfig>();
        list.add(lc);

        listener.setCollectorConfigs(list);
        listener.testStarted();

        long connectTime = 3123;
        long idleTime = 1233;
        long elapsedTime = 2213;
        long latency = 1532;
        int responseSize = 1342;
        String responseData =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.2-SNAPSHOT.20190506\">\n"
                + "  <hashTree>\n"
                + "    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan\" enabled=\"true\">\n"
                + "      <stringProp name=\"TestPlan.comments\"></stringProp>\n"
                + "      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n"
                + "      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n"
                + "      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n"
                + "      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" "
                + "guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n"
                + "        <collectionProp name=\"Arguments.arguments\"/>\n" + "      </elementProp>\n"
                + "      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n" + "    </TestPlan>\n"
                + "    <hashTree>\n" + "      <hashTree>\n"
                + "        <kg.apc.jmeter.samplers.DummySampler guiclass=\"kg.apc.jmeter.samplers.DummySamplerGui\" "
                + "testclass=\"kg.apc.jmeter.samplers.DummySampler\" testname=\"jp@gc - Dummy Sampler\" enabled=\"true\">\n"
                + "          <boolProp name=\"WAITING\">true</boolProp>\n"
                + "          <boolProp name=\"SUCCESFULL\">true</boolProp>\n"
                + "          <stringProp name=\"RESPONSE_CODE\">200</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_MESSAGE\">OK</stringProp>\n"
                + "          <stringProp name=\"REQUEST_DATA\">{&quot;email&quot;:&quot;user1&quot;, &quot;password&quot;:&quot;password1&quot;}；"
                + "</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_DATA\">{&quot;successful&quot;: true, &quot;account_id&quot;:&quot;0123456789&quot;}</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_TIME\">${__Random(50,500)}</stringProp>\n"
                + "          <stringProp name=\"LATENCY\">${__Random(1,50)}</stringProp>\n"
                + "          <stringProp name=\"CONNECT\">${__Random(1,5)}</stringProp>\n"
                + "        </kg.apc.jmeter.samplers.DummySampler></hashTree></hashTree>\n"
                + "  </hashTree></jmeterTestPlan><hashTree/></hashTree>\n" + "</jmeterTestPlan>";
        String failureMessage = "It's wrong message";

        ResultAndVariables res = TestUtilities.resultWithLabels();
        res.result.setConnectTime(connectTime);
        res.result.setResponseData(responseData.getBytes(StandardCharsets.UTF_8));
        res.result.setLatency(latency);
        res.result.setIdleTime(idleTime);
        res.result.setStampAndTime(System.currentTimeMillis(), elapsedTime);
        res.result.setSuccessful(true);
        AssertionResult ar = new AssertionResult("AssertionResult");
        ar.setFailure(true);
        ar.setFailureMessage(failureMessage);
        res.result.addAssertionResult(ar);


        SampleEvent event = new SampleEvent(res.result, "tg1", res.vars);
        listener.sampleOccurred(event); // 1st event, successful

    }
    @Test
    public void testLabelSuccess() throws InterruptedException {
        PrometheusListener listener = new PrometheusListener();

        BaseCollectorConfig cfg = TestUtilities.simpleGaugeCfg();
        cfg.setLabels(new String[] {"success","code","label","threadgroup"});

        ListenerCollectorConfig lc = new ListenerCollectorConfig(cfg);
        lc = TestUtilities.redoNameAndMeasuring(lc, "test_summary_idle_time", ListenerCollectorConfig.Measurable.Latency);
        List<ListenerCollectorConfig> list = new ArrayList<ListenerCollectorConfig>();
        list.add(lc);

        listener.setCollectorConfigs(list);
        listener.testStarted();

        long connectTime = 3123;
        long idleTime = 1233;
        long elapsedTime = 2213;
        long latency = 1532;
        int responseSize = 1342;
        String failureMessage = "It's wrong message";

        ResultAndVariables res = TestUtilities.resultWithLabels();
        res.result.setConnectTime(connectTime);
        res.result.setResponseData(new byte[responseSize]);
        res.result.setLatency(latency);
        res.result.setIdleTime(idleTime);
        res.result.setStampAndTime(System.currentTimeMillis(), elapsedTime);
        res.result.setSuccessful(true);
        AssertionResult ar = new AssertionResult("AssertionResult");
        ar.setFailure(true);
        ar.setFailureMessage(failureMessage);
        res.result.addAssertionResult(ar);

        SampleEvent event = new SampleEvent(res.result, "tg1", res.vars);
        listener.sampleOccurred(event); // 1st event, successful

    }

    public void updateAllTypes() {

        PrometheusListener listener = new PrometheusListener();
        List<ListenerCollectorConfig> configs = TestUtilities.fullListListener();

        for (ListenerCollectorConfig cfg : configs) {
            String aa = cfg.getMetricName();
            if (aa.equalsIgnoreCase("test_Info_failureMessage")) {
                cfg.setLabels(TestUtilities.TEST_LABELS_2);
            } else {
                cfg.setLabels(TestUtilities.TEST_LABELS);
            }
        }

        listener.setCollectorConfigs(configs);
        listener.testStarted();

        long connectTime = 3123;
        long idleTime = 1233;
        long elapsedTime = 2213;
        long latency = 1532;
        int responseSize = 1342;
        int samplesOccurred = 0;
        String failureMessage = "It's wrong message";

        ResultAndVariables res = TestUtilities.resultWithLabels();
        res.result.setConnectTime(connectTime);
        res.result.setResponseData(new byte[responseSize]);
        res.result.setLatency(latency);
        res.result.setIdleTime(idleTime);
        res.result.setStampAndTime(System.currentTimeMillis(), elapsedTime);
        res.result.setSuccessful(true);
        AssertionResult ar = new AssertionResult("AssertionResult");
        ar.setFailure(true);
        ar.setFailureMessage(failureMessage);
        res.result.addAssertionResult(ar);

        SampleEvent event = new SampleEvent(res.result, "tg1", res.vars);
        listener.sampleOccurred(event); // 1st event, successful
        samplesOccurred++;

        res.result.setSuccessful(false); // 2nd event, failure

        event = new SampleEvent(res.result, "tg1", res.vars);
        listener.sampleOccurred(event);
        samplesOccurred++;

        for (ListenerCollectorConfig cfg : configs) {
            String name = cfg.getMetricName();
            switch (name) {

                case "test_count_total":
                    Counter counter = (Counter) reg.getOrCreateAndRegister(cfg);
                    double shouldBeTwo = counter.labels(TestUtilities.EXPECTED_LABELS).get();
                    Assert.assertEquals(2.0, shouldBeTwo, 0.1);
                    break;
                case "test_failure_total":
                    counter = (Counter) reg.getOrCreateAndRegister(cfg);
                    double shouldBeOne = counter.labels(TestUtilities.EXPECTED_LABELS).get();
                    Assert.assertEquals(1.0, shouldBeOne, 0.1);
                    break;
                case "test_success_total":
                    counter = (Counter) reg.getOrCreateAndRegister(cfg);
                    shouldBeOne = counter.labels(TestUtilities.EXPECTED_LABELS).get();
                    Assert.assertEquals(1.0, shouldBeOne, 0.1);
                    break;
                case "test_ratio":
                    assertOnRatio(cfg);
                    break;

                // histograms
                case "test_hist_rtime":
                    assertOnHistogram(reg.getOrCreateAndRegister(cfg), elapsedTime * samplesOccurred, samplesOccurred,
                            elapsedTime);
                    break;
                case "test_hist_rsize":
                    assertOnHistogram(reg.getOrCreateAndRegister(cfg), responseSize * samplesOccurred, samplesOccurred,
                            responseSize);
                    break;
                case "test_hist_latency":
                    assertOnHistogram(reg.getOrCreateAndRegister(cfg), latency * samplesOccurred, samplesOccurred, latency);
                    break;
                case "test_hist_idle_time":
                    assertOnHistogram(reg.getOrCreateAndRegister(cfg), idleTime * samplesOccurred, samplesOccurred,
                            idleTime);
                    break;
                case "test_hist_connect_time":
                    assertOnHistogram(reg.getOrCreateAndRegister(cfg), connectTime * samplesOccurred, samplesOccurred,
                            connectTime);
                    break;

                // summaries
                case "test_summary_rtime":
                    assertOnSummary(reg.getOrCreateAndRegister(cfg), elapsedTime * samplesOccurred, samplesOccurred,
                            elapsedTime);
                    break;
                case "test_summary_rsize":
                    assertOnSummary(reg.getOrCreateAndRegister(cfg), responseSize * samplesOccurred, samplesOccurred,
                            responseSize);
                    break;
                case "test_summary_latency":
                    assertOnSummary(reg.getOrCreateAndRegister(cfg), latency * samplesOccurred, samplesOccurred, latency);
                    break;
                case "test_summary_idle_time":
                    assertOnSummary(reg.getOrCreateAndRegister(cfg), idleTime * samplesOccurred, samplesOccurred, idleTime);
                    break;
                case "test_summary_connect_time":
                    assertOnSummary(reg.getOrCreateAndRegister(cfg), connectTime * samplesOccurred, samplesOccurred,
                            connectTime);
                    break;

                default:
                    Assert.fail(name + " triggered untested switch case");
                    break;
            }
        }

        listener.testEnded();
    }

    private void assertOnRatio(ListenerCollectorConfig cfg) {
        SuccessRatioCollector ratio = (SuccessRatioCollector) reg.getOrCreateAndRegister(cfg);
        double shouldBeOne = ratio.getSuccess(TestUtilities.EXPECTED_LABELS);
        Assert.assertEquals(1.0, shouldBeOne, 0.1);

        shouldBeOne = ratio.getFailure(TestUtilities.EXPECTED_LABELS);
        Assert.assertEquals(1.0, shouldBeOne, 0.1);

        double shouldBeTwo = ratio.getTotal(TestUtilities.EXPECTED_LABELS);
        Assert.assertEquals(2.0, shouldBeTwo, 0.1);
    }

    protected static void assertOnHistogram(Collector collector, double expectedSum, double expectedCount,
                                            double boundary) {
        List<MetricFamilySamples> metrics = collector.collect();
        Assert.assertEquals(1, metrics.size());
        MetricFamilySamples family = metrics.get(0);

        // labels + Inf + count + sum
//		Assert.assertEquals(TestUtilities.EXPECTED_LABELS.length + 4, family.samples.size());

        for (Sample sample : family.samples) {
            List<String> values = sample.labelValues;
            List<String> names = sample.labelNames;

            // correct labels without 'le' (bin size)
            for (int i = 0; i < TestUtilities.TEST_LABELS.length; i++) {
                Assert.assertEquals(TestUtilities.TEST_LABELS[i], names.get(i));
                Assert.assertEquals(TestUtilities.EXPECTED_LABELS[i], values.get(i));
            }

            int labelSize = TestUtilities.EXPECTED_LABELS.length;

            // _sum and _count don't have an 'le' label
            if (sample.name.endsWith("count")) {
                Assert.assertTrue(values.size() == labelSize && names.size() == labelSize);
                Assert.assertEquals(expectedCount, sample.value, 0.1);

            } else if (sample.name.endsWith("sum")) {
                Assert.assertTrue(values.size() == labelSize && names.size() == labelSize);
                Assert.assertEquals(expectedSum, sample.value, 0.1);

            } else {
                Assert.assertEquals(values.size(), labelSize + 1);
                Assert.assertEquals(names.size(), labelSize + 1);

                String leString = values.get(labelSize);

                double le = (!leString.isEmpty() && !leString.equals("+Inf")) ? Double.parseDouble(leString)
                        : Double.MAX_VALUE;

                if (le == Double.MAX_VALUE) {
                    Assert.assertEquals(expectedCount, sample.value, 0.1);
                } else if (le < boundary) {
                    Assert.assertEquals(0, sample.value, 0.1);
                } else if (le > boundary) {
                    Assert.assertEquals(expectedCount, sample.value, 0.1);
                }
            }
        }
    }

    protected void assertOnSummary(Collector collector, double expectedSum, double expectedCount,
                                   double expectedValue) {

        List<MetricFamilySamples> metrics = collector.collect();
        Assert.assertEquals(1, metrics.size());
        MetricFamilySamples family = metrics.get(0);
        Assert.assertEquals(5, family.samples.size()); // 3 quantiles + count + sum

        for (Sample sample : family.samples) {
            List<String> values = sample.labelValues;
            List<String> names = sample.labelNames;

            for (int i = 0; i < TestUtilities.TEST_LABELS.length; i++) {
                Assert.assertEquals(TestUtilities.TEST_LABELS[i], names.get(i));
                Assert.assertEquals(TestUtilities.EXPECTED_LABELS[i], values.get(i));
            }

            // _sum and _count don't have an 'le' label
            if (sample.name.endsWith("count")) {
                Assert.assertEquals(values.size(), TestUtilities.EXPECTED_LABELS.length);
                Assert.assertEquals(names.size(), TestUtilities.TEST_LABELS.length);

                Assert.assertEquals(expectedCount, sample.value, 0.1);

            } else if (sample.name.endsWith("sum")) {
                Assert.assertEquals(expectedSum, sample.value, 0.1);
            } else {
                Assert.assertEquals(values.size(), TestUtilities.EXPECTED_LABELS.length + 1);
                Assert.assertEquals(values.size(), TestUtilities.EXPECTED_LABELS.length + 1);

                Assert.assertEquals(expectedValue, sample.value, 0.1);
            }
        }

    }


    public void testScheduleLatency() throws InterruptedException {
//        PrometheusListener listener = new PrometheusListener();
//
//        BaseCollectorConfig cfg = TestUtilities.simpleGaugeCfg();
//        cfg.setLabels(new String[] {"label","threadgroup"});
//
//        ListenerCollectorConfig lc = new ListenerCollectorConfig(cfg);
//        lc = TestUtilities.redoNameAndMeasuring(lc, "test_latency", ListenerCollectorConfig.Measurable.Latency);
//        List<ListenerCollectorConfig> list = new ArrayList<ListenerCollectorConfig>();
//        list.add(lc);

        PrometheusListener listener = new PrometheusListener();
        List<ListenerCollectorConfig> list = TestUtilities.fullListListener();

        for (ListenerCollectorConfig cfg : list) {
            String aa = cfg.getMetricName();
            if (aa.equalsIgnoreCase("simple_assert")) {
                cfg.setLabels(TestUtilities.TEST_LABELS_2);
            }
            if (aa.equalsIgnoreCase("test_summary_latency")) {
                cfg.setLabels(TestUtilities.TEST_LABELS_3);
            }

        }

        listener.setCollectorConfigs(list);
        listener.testStarted();

        long connectTime = 3123;
        long idleTime = 1233;
        long elapsedTime = 2213;
        int responseSize = 1342;
        String responseData =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.2-SNAPSHOT.20190506\">\n"
                + "  <hashTree>\n"
                + "    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan\" enabled=\"true\">\n"
                + "      <stringProp name=\"TestPlan.comments\"></stringProp>\n"
                + "      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n"
                + "      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n"
                + "      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n"
                + "      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" "
                + "guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n"
                + "        <collectionProp name=\"Arguments.arguments\"/>\n" + "      </elementProp>\n"
                + "      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n" + "    </TestPlan>\n"
                + "    <hashTree>\n" + "      <hashTree>\n"
                + "        <kg.apc.jmeter.samplers.DummySampler guiclass=\"kg.apc.jmeter.samplers.DummySamplerGui\" "
                + "testclass=\"kg.apc.jmeter.samplers.DummySampler\" testname=\"jp@gc - Dummy Sampler\" enabled=\"true\">\n"
                + "          <boolProp name=\"WAITING\">true</boolProp>\n"
                + "          <boolProp name=\"SUCCESFULL\">true</boolProp>\n"
                + "          <stringProp name=\"RESPONSE_CODE\">200</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_MESSAGE\">OK</stringProp>\n"
                + "          <stringProp name=\"REQUEST_DATA\">{&quot;email&quot;:&quot;user1&quot;, &quot;password&quot;:&quot;password1&quot;}；"
                + "</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_DATA\">{&quot;successful&quot;: true, &quot;account_id&quot;:&quot;0123456789&quot;}</stringProp>\n"
                + "          <stringProp name=\"RESPONSE_TIME\">${__Random(50,500)}</stringProp>\n"
                + "          <stringProp name=\"LATENCY\">${__Random(1,50)}</stringProp>\n"
                + "          <stringProp name=\"CONNECT\">${__Random(1,5)}</stringProp>\n"
                + "        </kg.apc.jmeter.samplers.DummySampler></hashTree></hashTree>\n"
                + "  </hashTree></jmeterTestPlan><hashTree/></hashTree>\n" + "</jmeterTestPlan>";
        String failureMessage = "It's wrong message";

        for (int a = 0; a < 5; a++) {
            Thread.sleep(100);

            new Thread(()->{
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Random r = new Random();
                ResultAndVariables res = TestUtilities.resultWithLabels();
                res.result.setConnectTime(connectTime);
                res.result.setResponseData(responseData.getBytes(StandardCharsets.UTF_8));
                res.result.setLatency(r.nextInt(1000)+1000);
                res.result.setIdleTime(idleTime);
                res.result.setStampAndTime(System.currentTimeMillis(), elapsedTime);
                res.result.setSuccessful(true);
                AssertionResult ar = new AssertionResult("AssertionResult");
                ar.setFailure(true);
                ar.setFailureMessage(failureMessage);
                res.result.addAssertionResult(ar);
                SampleEvent event = new SampleEvent(res.result, Thread.currentThread().getName(), res.vars);
                listener.sampleOccurred(event); // 1st event, successful
            }
            }).start();
        }
        Thread.sleep(100000);
    }
}
