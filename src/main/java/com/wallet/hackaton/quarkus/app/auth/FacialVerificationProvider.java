package com.wallet.hackaton.quarkus.app.auth;

@FunctionalInterface
public interface FacialVerificationProvider {
    FacialVerificationResult result();
}