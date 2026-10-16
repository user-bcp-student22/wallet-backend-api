package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.HttpHeaders;

@Path("/v1/onboarding/facial-verifications")
@IfBuildProfile(anyOf = {"dev", "test"})
@Produces(MediaType.APPLICATION_JSON)
public class FacialVerificationResource {
    private final FacialVerificationService service;
    private final BearerTokenAuthenticator tokenAuthenticator;

    @Inject
    public FacialVerificationResource(
            FacialVerificationService service,
            BearerTokenAuthenticator tokenAuthenticator) {
        this.service = service;
        this.tokenAuthenticator = tokenAuthenticator;
    }

    @POST
    public FacialVerificationResponse verify(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.verify(tokenAuthenticator.accountId(authorization));
    }
}