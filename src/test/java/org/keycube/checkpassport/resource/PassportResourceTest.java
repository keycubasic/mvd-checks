package org.keycube.checkpassport.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class PassportResourceTest {

    @Test
    void checkByHibernateFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-hibernate?serial=9922&number=987654")
                .then()
                .statusCode(302);
    }

    @Test
    void checkByHibernateNotFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-hibernate?serial=9922&number=987653")
                .then()
                .statusCode(404);
    }

    @Test
    void checkByElasticFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-elastic?serial=9922&number=987654")
                .then()
                .statusCode(302);
    }

    @Test
    void checkByElasticNotFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-elastic?serial=9922&number=987653")
                .then()
                .statusCode(404);
    }

    @Test
    void checkByPostgresFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-postgres?serial=9922&number=987654")
                .then()
                .statusCode(302);
    }

    @Test
    void checkByPostgresNotFound() {
        given()
                .when()
                .get("/check-passport/expired/check-by-postgres?serial=9922&number=987653")
                .then()
                .statusCode(404);
    }
}