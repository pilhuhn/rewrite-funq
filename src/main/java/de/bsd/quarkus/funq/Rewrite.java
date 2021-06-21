package de.bsd.quarkus.funq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Rewrite {

    @Inject
    @ConfigProperty(name = "rules")
    String rulesEnv;

    @Funq
    @CloudEventMapping(trigger = "com.redhat.cloud.notification", responseSource = "rewriter", responseType = "notification")
    // TODO Figure out how to use the avro Action object from notification-backend
    //      This probably needs some special Jackson configuration
    public Map<String,Object> notification(Map<String,Object> input, @Context CloudEvent eventInfo) throws JsonProcessingException {

        System.out.println("got >>" + input + "<<");

        System.out.println("With context >> " + eventInfo );
        System.out.println("Rules >>" + rulesEnv + "<<");
        printChars(rulesEnv);
        System.out.print("Separator ");
        String sep = System.lineSeparator();
        printChars(sep);


        // Map payload = input.getEvents().get(0).getPayload();
        List events = (List) input.get("events");
        Map<String,Object> tmpMap = (Map<String, Object>) events.get(0);
        String jsonPayload = (String) tmpMap.get("payload");
        Map<String, Object> payload = new ObjectMapper().readValue(jsonPayload, Map.class);

        String[] rules = rulesEnv.split(System.lineSeparator());
        for (String rule : rules) {
            System.out.println("Found rule >>" + rule + "<<");
            var tmp = rule.trim();
            if (tmp.isEmpty()) {
                continue;
            }
            String[] parts = tmp.split(":");
            String key = parts[0].trim();
            if (key.startsWith("-")) {
                var tmp2 = key.substring(1);
                payload.remove(tmp2);
            } else {
                if (parts.length != 2) {
                    throw new IllegalStateException("Illegal rule " + tmp );
                }
                var value = eval(parts[1].trim(), eventInfo, payload);
                System.out.printf("Eval'd to %s -> %s %n" , key, value );
                payload.put(key, value);
            }
        }

        payload.put("_rewriter","was on");

        String modifiedPayload = new ObjectMapper().writeValueAsString(payload);

        tmpMap.put("payload", modifiedPayload);

        return input;
    }

    private void printChars(String in) {
        StringBuilder sb = new StringBuilder();
        for (byte b : in.getBytes(StandardCharsets.UTF_8)) {
            var tmp = String.format("%2x [%c]  " , b, b);
            sb.append(tmp);
        }
        System.out.println(sb.toString());
    }

    // Evaluate the entry on the RHS
    String eval(String in, CloudEvent ce, Map payload) {
        // TODO we should do a real parser here , but for now this is good enough

        StringBuilder sb = new StringBuilder();
        String[] entries = in.split(" ");
        for (String tmp : entries) {
            String entry = tmp.trim();

            if (entry.equals(".ce.source")) {
                sb.append(ce.source());
            }
            if (entry.equals(".ce.id")) {
                sb.append(ce.id());
            }
            if (entry.startsWith("$")) {
                String key = entry.substring(1);
                String tmp2 = (String) payload.get(key);
                if (tmp2 != null && !tmp2.isEmpty()) {
                    sb.append(tmp2);
                }
            }
        }

        String out = sb.toString();
        // No match? Return the input
        if (out.isEmpty()) {
            out = in;
        }
        return out;
    }
}
