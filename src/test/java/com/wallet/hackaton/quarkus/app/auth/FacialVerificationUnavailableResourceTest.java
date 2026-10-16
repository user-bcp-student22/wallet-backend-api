package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(FacialVerificationUnavailableResourceTest.UnavailableProfile.class)
class FacialVerificationUnavailableResourceTest extends FacialVerificationResourceTestSupport {
    @Inject
    AccountRepository repository;

    @Test
    void unavailableResultReturnsProblemAndDoesNotMutateOnboarding() {
        TestSession session = createSession(true);

        given()
                .auth().oauth2(session.accessToken())
                .when().post("/v1/onboarding/facial-verifications")
                .then()
                .statusCode(503)
                .contentType("application/problem+json");

        assertEquals("FACIAL_VERIFICATION_PENDING",
                repository.findById(session.userId()).orElseThrow().onboardingStatus());
    }

    public static class UnavailableProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("wallet.facial-verification.mock-result", "UNAVAILABLE");
        }
    }
}