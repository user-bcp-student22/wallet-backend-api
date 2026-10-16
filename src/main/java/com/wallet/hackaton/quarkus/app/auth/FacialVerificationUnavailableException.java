package com.wallet.hackaton.quarkus.app.auth;

public class FacialVerificationUnavailableException extends AuthProblemException {
    public FacialVerificationUnavailableException() {
        super(503, "Simulación no disponible", "FACIAL_VERIFICATION_UNAVAILABLE",
                "La simulación de demo no está disponible.");
    }
}