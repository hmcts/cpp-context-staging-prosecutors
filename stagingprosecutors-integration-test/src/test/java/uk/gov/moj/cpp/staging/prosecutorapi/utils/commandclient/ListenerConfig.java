package uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ListenerConfig {
    String key();

    ListeningStrategy until() default ListeningStrategy.UNTIL_RECEIVAL;

    long timeout() default 10000;
}
