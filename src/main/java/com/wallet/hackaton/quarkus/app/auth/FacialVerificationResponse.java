package com.wallet.hackaton.quarkus.app.auth;

public record FacialVerificationResponse(
        FacialVerificationResult result,
        AuthSessionResponse.Onboarding onboarding) {
}