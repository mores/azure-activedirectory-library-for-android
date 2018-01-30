package com.microsoft.aad.adal;

public class AuthContextConfig {

    public static void setSkipBrokerAccountService(final AuthenticationContext context, boolean shouldSkip) {
        context.setSkipBrokerAccountService(shouldSkip);
    }

}
