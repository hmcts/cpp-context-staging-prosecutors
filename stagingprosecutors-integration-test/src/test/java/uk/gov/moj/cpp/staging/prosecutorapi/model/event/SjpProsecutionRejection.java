package uk.gov.moj.cpp.staging.prosecutorapi.model.event;


import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.Event;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventConsumer;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventEmitter;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import lombok.Builder;

@Builder
@Event(name = "public.prosecutioncasefile.prosecution-rejected", topic = "jms.topic.public.event")
public class SjpProsecutionRejection {

    @Builder.Default
    public String caseId = UUID.randomUUID().toString();

    @Builder.Default
    public String externalId;

    @Builder.Default
    public String channel = "CPPI";

    @Builder.Default
    public List<Problem> errors = ImmutableList.of(Problem.builder().build());

    public EventEmitter emitter() {
        return new EventEmitter(this);
    }

    public static EventConsumer<SjpProsecutionRejection> consumer() {
        return new EventConsumer<>(SjpProsecutionRejection.class);
    }

}
