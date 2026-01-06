package uk.gov.moj.cpp.staging.prosecutorapi.utils;

import uk.gov.justice.services.messaging.JsonObjects;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.UUIDValidator.isValidUUID;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.JsonObjectsHelper.readFromString;

import uk.gov.moj.cpp.staging.prosecutors.test.utils.JsonObjectsHelper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.Awaitility;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ProsecutionCaseFileApi {

    public static void expectInitiateSjpProsecutionInvokedWith(final String resourceName) {
        Awaitility
                .await()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> createInitiateSjpProsecutionInvokedWith(ResourcesUtils.asJsonObject(resourceName)));
    }

    public static void expectInitiateSjpProsecutionInvokedWith(final JsonObject resource) {
        Awaitility
                .await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> createInitiateSjpProsecutionInvokedWith(resource));
    }

    public static void expectInitiateProsecutionInvokedWith(final String resourceName) {
        Awaitility
                .await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> createInitiateCCProsecutionInvokedWith(ResourcesUtils.asJsonObject(resourceName)));
    }

    private static boolean createInitiateSjpProsecutionInvokedWith(final JsonObject expectedPayload) {
        final RequestPatternBuilder request = postRequestedFor(
                urlPathEqualTo("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-sjp-prosecution"))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.prosecutioncasefile.command.initiate-sjp-prosecution+json"));

        return validateJson(expectedPayload, request);
    }

    private static boolean createInitiateCCProsecutionInvokedWith(final JsonObject expectedPayload) {
        final RequestPatternBuilder request = postRequestedFor(
                urlPathEqualTo("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution"))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.prosecutioncasefile.command.initiate-cc-prosecution+json"));

        return validateJson(expectedPayload, request);
    }

    private static boolean validateJson(final JsonObject expectedPayload, final RequestPatternBuilder request) {
        if (findAll(request).isEmpty()) {
            return false;
        } else {
            final Optional<JsonObject> actualJsonObjectPayload = findAll(request)
                    .stream()
                    .filter(actualRequest -> {
                        final String actualSjpPayload = actualRequest.getBodyAsString();
                        final JsonObject payloadJsonObject = readFromString(actualSjpPayload);
                        final JsonObject caseDetails = payloadJsonObject.getJsonObject("caseDetails");
                        final JsonString caseReferenceJson = caseDetails.getJsonString("prosecutorCaseReference");
                        if (caseReferenceJson != null) {
                            return caseReferenceJson.getString().equals("TVL54321");
                        } else {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(LoggedRequest::getBodyAsString)
                    .map(JsonObjectsHelper::readFromString);

            if (actualJsonObjectPayload.isPresent()) {
                assertEquals(expectedPayload.toString(), actualJsonObjectPayload.get().toString(), new CustomComparator(STRICT,
                        new Customization("defendants[0].offences[0].offenceId", (o1, o2) -> true),
                        new Customization("defendants[0].offences[1].offenceId", (o1, o2) -> true),
                        new Customization("defendants[1].offences[0].offenceId", (o1, o2) -> true),
                        new Customization("defendants[1].offences[1].offenceId", (o1, o2) -> true),
                        new Customization("defendants[2].offences[0].offenceId", (o1, o2) -> true),
                        new Customization("defendants[2].offences[1].offenceId", (o1, o2) -> true),
                        new Customization("defendants[0].id", (o1, o2) -> true),
                        new Customization("defendants[1].id", (o1, o2) -> true),
                        new Customization("defendants[2].id", (o1, o2) -> true),
                        new Customization("externalId", (o1, o2) -> isValidUUID.apply(o1.toString())),
                        new Customization("_metadata", (o1, o2) -> true),

                        new Customization("caseDetails.prosecutorCaseReference", (o1, o2) -> true),
                        new Customization("caseDetails.caseId", (o1, o2) -> true),
                        new Customization("caseDetails.dateReceived", (o1, o2) -> true)
                ));
                return true;
            } else {
                return false;
            }
        }
    }
}
