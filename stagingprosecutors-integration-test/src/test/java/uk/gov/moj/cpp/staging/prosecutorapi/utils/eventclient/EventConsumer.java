package uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

public class EventConsumer<T> {

    @Getter
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();

    private final Class<T> eventClass;

    public EventConsumer(Class<T> eventClass) {
        if (!eventClass.isAnnotationPresent(Event.class)) {
            throw new IllegalArgumentException("EventConsumer can only be used with the classes decorated with Event annotation.");
        }

        this.eventClass = eventClass;
    }

}
