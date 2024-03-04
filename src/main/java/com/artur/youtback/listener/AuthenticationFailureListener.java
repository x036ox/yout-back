package com.artur.youtback.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFailureListener.class);
    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        logger.warn("Authentication denied: " + event.getException());
    }
}
