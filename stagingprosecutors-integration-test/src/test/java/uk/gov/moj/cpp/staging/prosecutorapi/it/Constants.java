package uk.gov.moj.cpp.staging.prosecutorapi.it;


public class Constants {
    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "jms.topic.public.event";
    public static final String PRIVATE_ACTIVE_MQ_TOPIC = "jms.topic.stagingprosecutors.event";
    public static final Integer MESSAGE_QUEUE_TIMEOUT = 10000;

    public static final String PRIVATE_SJP_PROSECUTION_RECEIVED = "stagingprosecutors.event.sjp-prosecution-received";

}