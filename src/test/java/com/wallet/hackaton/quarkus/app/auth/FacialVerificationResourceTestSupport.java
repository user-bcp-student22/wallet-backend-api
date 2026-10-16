package com.wallet.hackaton.quarkus.app.auth;

import io.restassured.response.Response;
import jakarta.inject.Inject;

import java.util.UUID;

import static io.restassured.RestAssured.given;

abstract class FacialVerificationResourceTestSupport {
    @Inject
    AuthSessionService authSessionService;

    protected TestSession createSession(boolean completePasskey) {
        String idToken = "mock-google-token-facial-" + UUID.randomUUID();
        Response response = given()
                .contentType("application/json")
                .body("{\"idToken\":\"" + idToken + "\",\"deviceId\":\"facial-test-device\"}")
                .when().post("/v1/auth/google")
                .then()
                .statusCode(200)
                .extract().response();

        String userId = response.path("user.id");
        if (completePasskey) {
            authSessionService.completePasskeyRegistration(userId);
        }
        String accessToken = response.path("accessToken");
        return new TestSession(accessToken, userId);
    }

    protected record TestSession(String accessToken, String userId) {
    }
}