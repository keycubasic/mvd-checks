package org.keycube.checkpassport.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.keycube.checkpassport.model.Passport;
import org.keycube.checkpassport.service.PassportService;

import java.time.LocalDateTime;

@Path("expired")
public class PassportResource {

    @Inject
    PassportService service;

    @GET
    @Path("download")
    @Operation(hidden = true)
    public Response download() {
        service.download();
        return Response.ok().build();
    }

    @GET
    @Path("decompress")
    @Operation(hidden = true)
    public Response decompress() {
        service.decompress();
        return Response.ok().build();
    }

    @GET
    @Path("persist-new")
    @Operation(hidden = true)
    public Response persistNew() {
        service.persist(false, true);
        return Response.ok().build();
    }

    @GET
    @Path("persist-all")
    @Operation(hidden = true)
    public Response persistAll() {
        LocalDateTime date = service.persist(false, false);
        Passport.deleteBeforeDate(date);
        return Response.ok().build();
    }

    @GET
    @Path("persist-test-new")
    @Operation(hidden = true)
    public Response persistTestNew() {
        service.persist(true, true);
        return Response.ok().build();
    }

    @GET
    @Path("persist-test-all")
    @Operation(hidden = true)
    public Response persistTestAll() {
        LocalDateTime date = service.persist(true, false);
        Passport.deleteBeforeDate(date);
        return Response.ok().build();
    }

    @GET
    @Path("check")
    @Operation(
            summary = "Проверить паспорт",
            description = "Укажите данные паспорта"
    )
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Паспорт не найден в списке недействительных российских паспортов"
                    ),
                    @APIResponse(
                            responseCode = "302",
                            description = "Паспорт найден в списке недействительных российских паспортов"
                    )
            }
    )
    public Response check(@Parameter(description = "Серия паспорта", required = true)
                              @QueryParam("serial") String serial,
                          @Parameter(description = "Номер паспорта", required = true)
                              @QueryParam("number") String number) {
        Response.Status status = Response.Status.NOT_FOUND;
        if (Passport.checkExpired(serial.trim(), number.trim())) {
            status = Response.Status.FOUND;
        }
        return Response.status(status).build();
    }
}
