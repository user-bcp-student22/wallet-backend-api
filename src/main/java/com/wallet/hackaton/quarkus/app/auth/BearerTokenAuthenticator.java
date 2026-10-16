package com.wallet.hackaton.quarkus.app.auth;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.keys.HmacKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class BearerTokenAuthenticator {
    private final JwtConsumer jwtConsumer;

    @Inject
    public BearerTokenAuthenticator(
            @ConfigProperty(name = "wallet.auth.jwt.secret") String signingSecret) {
        this.jwtConsumer = new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setRequireSubject()
            .setExpectedIssuer(SessionTokenIssuer.ISSUER)
            .setAllowedClockSkewInSeconds(60)
            .setVerificationKey(new HmacKey(signingSecret.getBytes(StandardCharsets.UTF_8)))
            .setJwsAlgorithmConstraints(new AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.PERMIT,
                AlgorithmIdentifiers.HMAC_SHA256))
            .build();
    }

    public String accountId(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new InvalidCredentialsException();
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        try {
            String subject = jwtConsumer.processToClaims(token).getSubject();
            if (subject == null || subject.isBlank()) {
                throw new InvalidCredentialsException();
            }
            return subject;
        } catch (InvalidJwtException | MalformedClaimException exception) {
            throw new InvalidCredentialsException();
        }
    }
}