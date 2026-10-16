package com.wallet.hackaton.quarkus.app.auth;

public class InvalidOnboardingStateException extends AuthProblemException {
    public InvalidOnboardingStateException() {
        super(409, "Estado de onboarding inválido", "INVALID_ONBOARDING_STATE",
                "La operación no corresponde al paso actual del registro.");
    }
}