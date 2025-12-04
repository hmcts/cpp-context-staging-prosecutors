package uk.gov.justice.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("v1/prosecutions/cps-xml/materials")
public interface CommandApiV1ProsecutionsCpsXmlMaterialsResource {
  @POST
  @Produces("application/json")
  @Consumes(MediaType.APPLICATION_XML)
  Response postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials(String payload);
}
