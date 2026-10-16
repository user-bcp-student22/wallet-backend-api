package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class FacialVerificationResourceTest extends FacialVerificationResourceTestSupport {
    @Inject
    BearerTokenAuthenticator tokenAuthenticator;

    @Test
    void approvedResultMovesAuthenticatedAccountToPhonePendingWithoutFacialData() {
        TestSession session = createSession(true);
        org.junit.jupiter.api.Assertions.assertEquals(
                session.userId(), tokenAuthenticator.accountId("Bearer " + session.accessToken()));

        given()
                .auth().oauth2(session.accessToken())
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(200)
                .body("result", equalTo("APPROVED"))
                .body("onboarding.status", equalTo("PHONE_PENDING"))
                .body("onboarding.nextAction", equalTo("ENTER_PHONE"))
                .body("$", not(hasKey("image")))
                .body("$", not(hasKey("template")));
    }

    @Test
    void requiresBearerToken() {
        given()
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    void rejectsInvalidBearerToken() {
        given()
                .auth().oauth2("invalid.jwt.token")
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    void rejectsRequestBeforePasskeyCompletion() {
        TestSession session = createSession(false);

        given()
                .auth().oauth2(session.accessToken())
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(409)
                .contentType("application/problem+json");
    }
}