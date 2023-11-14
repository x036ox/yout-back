package com.artur.youtback.listener;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthorizationDeniedEvent> {

    @Override
    public void onApplicationEvent(AuthorizationDeniedEvent event) {
        System.out.println(event.getAuthentication().get().getName());
    }
}
