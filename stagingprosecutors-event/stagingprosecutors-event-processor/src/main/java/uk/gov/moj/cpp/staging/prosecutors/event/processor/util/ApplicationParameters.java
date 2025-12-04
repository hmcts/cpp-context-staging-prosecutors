package uk.gov.moj.cpp.staging.prosecutors.event.processor.util;

import uk.gov.justice.services.common.configuration.Value;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

@SuppressWarnings("squid:S1488")
public class ApplicationParameters {

    @Inject
    @Value(key = "poca_application_cannot_be_read", defaultValue = "f937ed9f-a47c-41a4-805b-eb33706108b5")
    private String technicalErrorEmailTemplateId;

    @Inject
    @Value(key = "poca_applicant_details_missing", defaultValue = "9f7bc07d-cec9-4904-b769-c554cc013372")
    private String applicationEmailTemplateId;

    @Inject
    @Value(key = "poca_respondent_details_missing", defaultValue = "ec800fa3-c059-4f96-8887-a0807d41e41d")
    private String respondentEmailTemplateId;

    @Inject
    @Value(key = "poca_court_location_missing", defaultValue = "30a6057a-39d8-457b-9f53-1f4f2bf81995")
    private String courtLocationEmailTemplateId;

    @Inject
    @Value(key = "missing_mandatory_fields", defaultValue = "f12a0920-5253-4fff-a5be-f64b33c5f576")
    private String missingMandatoryEmailTemplateId;

    public String getEmailTemplateId(final String templateName) {
        final Map<String, String> emailTemplatesMap = new HashMap<>();
        emailTemplatesMap.put("poca_application_cannot_be_read", this.technicalErrorEmailTemplateId);
        emailTemplatesMap.put("poca_respondent_details_missing", this.respondentEmailTemplateId);
        emailTemplatesMap.put("poca_applicant_details_missing", this.applicationEmailTemplateId);
        emailTemplatesMap.put("poca_court_location_missing", this.courtLocationEmailTemplateId);
        emailTemplatesMap.put("missing_mandatory_fields", this.missingMandatoryEmailTemplateId);
        final String templateId = emailTemplatesMap.getOrDefault(templateName, "''");
        return templateId;
    }
}
