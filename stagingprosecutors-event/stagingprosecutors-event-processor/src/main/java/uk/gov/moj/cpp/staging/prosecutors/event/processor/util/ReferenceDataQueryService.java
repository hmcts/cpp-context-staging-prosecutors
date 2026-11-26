package uk.gov.moj.cpp.staging.prosecutors.event.processor.util;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;

import javax.json.JsonArray;
import java.util.Optional;

public interface ReferenceDataQueryService {

    ParentBundleSectionReferenceData getParentBundleSectionByCpsBundleCode(final Metadata metadata, final String cpsBundleCode);

    DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode);

    Optional<JsonArray> getCPSProsecutors(final JsonEnvelope event, final Requester requester);
}
