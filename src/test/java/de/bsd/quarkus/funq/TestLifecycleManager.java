package de.bsd.quarkus.funq;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {

        String sep = System.lineSeparator();
        String rules = "key3: .meta.source" + sep + "key1: $key1 + .meta.id" + sep + "-key2" + sep;

        HashMap<String, String> props = new HashMap<>();
        props.put("rules",rules);

        return props;
    }

    @Override
    public void stop() {

    }
}
