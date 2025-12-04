package uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DeclareJointListener {
    String key();

    String[] events();

    String topic();
}
