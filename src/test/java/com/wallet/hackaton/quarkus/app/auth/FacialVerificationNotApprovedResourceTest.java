package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(FacialVerificationNotApprovedResourceTest.NotApprovedProfile.class)
class FacialVerificationNotApprovedResourceTest extends FacialVerificationResourceTestSupport {
    @Test
    void notApprovedResultLeavesFacialStepPending() {
        TestSession session = createSession(true);

        given()
                .auth().oauth2(session.accessToken())
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(200)
                .body("result", equalTo("NOT_APPROVED"))
                .body("onboarding.status", equalTo("FACIAL_VERIFICATION_PENDING"))
                .body("onboarding.nextAction", equalTo("VERIFY_FACIAL_IDENTITY"));
    }

    public static class NotApprovedProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("wallet.facial-verification.mock-result", "NOT_APPROVED");
        }
    }
}