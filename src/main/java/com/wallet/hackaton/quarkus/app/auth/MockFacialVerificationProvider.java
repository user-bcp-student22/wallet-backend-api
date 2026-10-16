package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@IfBuildProfile(anyOf = {"dev", "test"})
public class MockFacialVerificationProvider implements FacialVerificationProvider {
    private final FacialVerificationResult configuredResult;

    @Inject
    public MockFacialVerificationProvider(
            @ConfigProperty(name = "wallet.facial-verification.mock-result", defaultValue = "APPROVED")
            FacialVerificationResult configuredResult) {
        this.configuredResult = configuredResult;
    }

    @Override
    public FacialVerificationResult result() {
        return configuredResult;
    }
}