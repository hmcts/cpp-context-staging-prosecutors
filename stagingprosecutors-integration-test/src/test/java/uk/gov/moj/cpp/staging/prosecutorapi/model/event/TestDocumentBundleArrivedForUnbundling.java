package uk.gov.moj.cpp.staging.prosecutorapi.model.event;


import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.Event;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventConsumer;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient.EventEmitter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CmsDocumentIdentifier;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;

import java.util.Optional;
import java.util.UUID;

import lombok.Builder;

@Builder
@Event(name = "public.prosecutioncasefile.document-bundle-arrived-for-unbundling", topic = "jms.topic.public.event")
public class TestDocumentBundleArrivedForUnbundling {

    public UUID caseId;

    public CmsDocumentIdentifier cmsDocumentIdentifier = CmsDocumentIdentifier.cmsDocumentIdentifier().build();

    public Material material = Material.material().build();

    public Optional<String> prosecutingAuthority;

    public String prosecutorDefendantId;

    public String defendantName;

    public String receivedDateTime;

    public EventEmitter emitter() {
        return new EventEmitter(this);
    }

    public static EventConsumer<TestDocumentBundleArrivedForUnbundling> consumer() {
        return new EventConsumer<>(TestDocumentBundleArrivedForUnbundling.class);
    }
}
