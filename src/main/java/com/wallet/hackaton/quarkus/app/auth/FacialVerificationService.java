package com.wallet.hackaton.quarkus.app.auth;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@IfBuildProfile(anyOf = {"dev", "test"})
public class FacialVerificationService {
    private final AccountRepository accountRepository;
    private final FacialVerificationProvider provider;

    @Inject
    public FacialVerificationService(
            AccountRepository accountRepository,
            FacialVerificationProvider provider) {
        this.accountRepository = accountRepository;
        this.provider = provider;
    }

    public FacialVerificationResponse verify(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(InvalidCredentialsException::new);
        if (!"FACIAL_VERIFICATION_PENDING".equals(account.onboardingStatus())) {
            throw new InvalidOnboardingStateException();
        }

        FacialVerificationResult result = provider.result();
        if (result == FacialVerificationResult.UNAVAILABLE) {
            throw new FacialVerificationUnavailableException();
        }

        Account updated = account;
        if (result == FacialVerificationResult.APPROVED) {
            updated = account.withOnboarding(true, "PHONE_PENDING", "ENTER_PHONE");
            accountRepository.update(updated);
        }
        return new FacialVerificationResponse(result, new AuthSessionResponse.Onboarding(
                updated.onboardingStatus(), updated.nextAction(), updated.phoneNumberMasked()));
    }
}