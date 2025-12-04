package uk.gov.moj.cpp.staging.prosecutorapi.model.event;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.Event;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventConsumer;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventEmitter;

import lombok.Builder;

@Builder
@Event(name = "public.progression.court-application-created", topic = "jms.topic.public.event")
public class TestCourtApplicationCreated {

    public CourtApplication courtApplication;

    public EventEmitter emitter() {
        return new EventEmitter(this);
    }

    public static EventConsumer<TestCourtApplicationCreated> consumer() {
        return new EventConsumer<>(TestCourtApplicationCreated.class);
    }
}
