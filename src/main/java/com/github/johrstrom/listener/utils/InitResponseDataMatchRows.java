package com.github.johrstrom.listener.utils;

import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;


public class InitResponseDataMatchRows {
    private static LinkedList keysList = null;
    private static final String VAR = "prometheus.rowsmatch";
    private static final String DEFALUT_VAR = "";
    private static final String ROWS = JMeterUtils.getPropDefault(VAR, DEFALUT_VAR);

    private static Logger log = LoggerFactory.getLogger(InitResponseDataMatchRows.class);

    public static LinkedList<String> getKeysList() {
        return keysList;
    }

    public static void initRows() {
        if (ROWS != "") {
            String[] matchRows = ROWS.split(",");
            keysList = new LinkedList(Arrays.asList(matchRows));
            log.debug("rowsMap init done size: {}", keysList.size());
        }

    }
}
