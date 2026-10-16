package com.wallet.hackaton.quarkus.app.auth;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class InMemoryAccountRepository implements AccountRepository {
    private final ConcurrentMap<IdentityKey, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account findOrCreate(String provider, GoogleIdentity identity) {
        // Email is profile data; linking accounts by email is outside this flow.
        var key = new IdentityKey(provider, identity.subject());
        return accounts.computeIfAbsent(key, ignored ->
                Account.newGoogleAccount(identity.subject(), identity.email(), identity.name()));
    }

    @Override
    public java.util.Optional<Account> findById(String accountId) {
        return accounts.values().stream()
                .filter(account -> account.id().equals(accountId))
                .findFirst();
    }

    @Override
    public void update(Account account) {
        var key = new IdentityKey(account.provider(), account.subject());
        accounts.computeIfPresent(key, (ignored, current) -> account);
    }

    private record IdentityKey(String provider, String subject) {
    }
}
