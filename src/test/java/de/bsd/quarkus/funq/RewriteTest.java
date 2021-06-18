package de.bsd.quarkus.funq;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
public class RewriteTest {

    @Test
    public void testFunq() {

        String jsonPayload = "{\"bundle\":\"my-bundle\",\"application\":\"my-app\",\"timestamp\":\"2021-06-17T11:51:51.354544\",\"payload\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"event_type\":\"a_type\",\"account_id\":\"123\"}";


        String id = UUID.randomUUID().toString();
        ResponseBodyExtractionOptions body = given().contentType("application/json")
                .header("ce-specversion", "1.0")
                .header("ce-id", id)
                .header("ce-type", "notification")
                .header("ce-source", "test")
                .body(jsonPayload)
                .post("/")
                .then().statusCode(200)
                .header("ce-id", notNullValue())
                .header("ce-type", "notification")
                .header("ce-source", "rewriter")
                .body(Matchers.allOf(containsString("_rewriter"), containsString("my-app")))
                .extract().body();

        System.out.println(body.asString());
        RestAction ra = body.as(RestAction.class);

        assertTrue(ra.payload.containsKey("key3"));
        assertTrue(!ra.payload.containsKey("key2"));
        assertEquals(ra.payload.get("key3"),"test" ); // ce-source of the input above
        assertEquals(ra.payload.get("key1"), "value1"+id);
    }

}
