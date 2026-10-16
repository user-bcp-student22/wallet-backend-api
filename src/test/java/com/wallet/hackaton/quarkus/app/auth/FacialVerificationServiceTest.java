package com.wallet.hackaton.quarkus.app.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacialVerificationServiceTest {
    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();
    private final AuthSessionService authSessionService =
            new AuthSessionService(new MockGoogleIdentityProvider(), repository);

    @Test
    void completedPasskeyStartsFacialVerificationWithoutChangingBiometricMeaning() {
        Account account = createAccount();

        var onboarding = authSessionService.completePasskeyRegistration(account.id());
        Account updated = repository.findById(account.id()).orElseThrow();

        assertTrue(updated.biometricRegistered());
        assertEquals("FACIAL_VERIFICATION_PENDING", onboarding.status());
        assertEquals("VERIFY_FACIAL_IDENTITY", onboarding.nextAction());
    }

    @Test
    void approvedResultAllowsContinuingToPhone() {
        Account account = accountAfterPasskey();
        FacialVerificationService service = new FacialVerificationService(
                repository, () -> FacialVerificationResult.APPROVED);

        var response = service.verify(account.id());

        assertEquals(FacialVerificationResult.APPROVED, response.result());
        assertEquals("PHONE_PENDING", response.onboarding().status());
        assertEquals("ENTER_PHONE", response.onboarding().nextAction());
    }

    @Test
    void notApprovedResultKeepsFacialVerificationPending() {
        Account account = accountAfterPasskey();
        FacialVerificationService service = new FacialVerificationService(
                repository, () -> FacialVerificationResult.NOT_APPROVED);

        var response = service.verify(account.id());

        assertEquals(FacialVerificationResult.NOT_APPROVED, response.result());
        assertEquals("FACIAL_VERIFICATION_PENDING", response.onboarding().status());
        assertEquals("VERIFY_FACIAL_IDENTITY", response.onboarding().nextAction());
    }

    @Test
    void unavailableResultDoesNotChangeOnboarding() {
        Account account = accountAfterPasskey();
        FacialVerificationService service = new FacialVerificationService(
                repository, () -> FacialVerificationResult.UNAVAILABLE);

        assertThrows(FacialVerificationUnavailableException.class,
                () -> service.verify(account.id()));
        assertEquals("FACIAL_VERIFICATION_PENDING",
                repository.findById(account.id()).orElseThrow().onboardingStatus());
    }

    @Test
    void verificationCannotBeRequestedBeforePasskeyCompletion() {
        Account account = createAccount();
        FacialVerificationService service = new FacialVerificationService(
                repository, () -> FacialVerificationResult.APPROVED);

        assertThrows(InvalidOnboardingStateException.class,
                () -> service.verify(account.id()));
    }

    private Account accountAfterPasskey() {
        Account account = createAccount();
        authSessionService.completePasskeyRegistration(account.id());
        return repository.findById(account.id()).orElseThrow();
    }

    private Account createAccount() {
        return authSessionService.authenticate("mock-google-token-facial-test").account();
    }
}