package com.elora.security.mfa;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import org.springframework.stereotype.Service;

@Service
public class MfaService {
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    public String generateSecret() { return new DefaultSecretGenerator().generate(); }
    public boolean verifyCode(String secret, String code) {
        try { return verifier.isValidCode(secret, code); } catch (Exception e) { return false; }
    }
}
