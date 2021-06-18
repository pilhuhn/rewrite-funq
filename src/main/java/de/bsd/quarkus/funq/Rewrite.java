package de.bsd.quarkus.funq;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Rewrite {

    @Inject
    @ConfigProperty(name = "rules")
    String rulesEnv;

    @Funq
    @CloudEventMapping(responseSource = "rewriter", responseType = "notification")
    public RestAction notification(RestAction input, @Context CloudEvent eventInfo) {

        System.out.println("got >>" + input + "<<");
        System.out.println("With context >> " + eventInfo );
        System.out.println("Rules >> " + rulesEnv);

        String[] rules = rulesEnv.split("\\\\n");
        for (String rule : rules) {
            var tmp = rule.trim();
            if (tmp.isEmpty()) {
                continue;
            }
            String[] parts = tmp.split(":");
            String key = parts[0].trim();
            if (key.startsWith("-")) {
                var tmp2 = key.substring(1);
                input.payload.remove(tmp2);
            } else {
                if (parts.length != 2) {
                    throw new IllegalStateException("Illegal rule " + tmp);
                }
                var value = eval(parts[1].trim(), eventInfo, input);
                input.payload.put(key, value);
            }
        }

        input.payload.put("_rewriter","was on");

        return input;
    }

    // Evaluate the entry on the RHS
    String eval(String in, CloudEvent ce, RestAction ra) {
        // TODO we should do a real parser here , but for now this is good enough

        StringBuilder sb = new StringBuilder();
        String[] entries = in.split(" ");
        for (String tmp : entries) {
            String entry = tmp.trim();

            if (entry.equals(".meta.source")) {
                sb.append(ce.source());
            }
            if (entry.equals(".meta.id")) {
                sb.append(ce.id());
            }
            if (entry.startsWith("$")) {
                String key = entry.substring(1);
                String tmp2 = (String) ra.payload.get(key);
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
