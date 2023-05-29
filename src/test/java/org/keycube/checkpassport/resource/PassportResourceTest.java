package org.keycube.checkpassport.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PassportResourceTest {

    @Test
    void checkFound() {
        given()
                .when()
                .get("/check-passport/expired/check?serial=9923&number=123456")
                .then()
                .statusCode(302);
    }

    @Test
    void checkNotFound() {
        given()
                .when()
                .get("/check-passport/expired/check?serial=9923&number=123455")
                .then()
                .statusCode(404);
    }
}