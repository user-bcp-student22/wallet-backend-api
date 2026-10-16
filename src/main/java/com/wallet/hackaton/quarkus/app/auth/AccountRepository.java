package com.wallet.hackaton.quarkus.app.auth;

import java.util.Optional;

public interface AccountRepository {
    Account findOrCreate(String provider, GoogleIdentity identity);

    Optional<Account> findById(String accountId);

    void update(Account account);
}
