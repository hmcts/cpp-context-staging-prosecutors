package uk.gov.justice.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("v1/prosecutions/cps-xml/materials")
public interface CommandApiV1ProsecutionsCpsXmlMaterialsResource {
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_XML)
  Response postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials(String payload);
}
