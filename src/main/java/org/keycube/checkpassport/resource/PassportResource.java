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
    @Path("split")
    @Operation(hidden = true)
    public Response split() {
        service.split();
        return Response.ok().build();
    }

    @GET
    @Path("persist")
    @Operation(hidden = true)
    public Response persist() {
        service.persist(false);
        return Response.ok().build();
    }

    @GET
    @Path("persist-test")
    @Operation(hidden = true)
    public Response persistTest() {
        service.persist(true);
        return Response.ok().build();
    }

    @GET
    @Path("check-by-hibernate")
    @Operation(
            summary = "Проверить паспорт (hibernate)",
            description = "Поиск посредством HQL"
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
    public Response checkByHibernate(@Parameter(description = "Серия паспорта", required = true)
                                     @QueryParam("serial") String serial,
                                     @Parameter(description = "Номер паспорта", required = true)
                                     @QueryParam("number") String number) {
        Response.Status status = Response.Status.NOT_FOUND;
        if (Passport.checkByHibernate(serial.trim(), number.trim())) {
            status = Response.Status.FOUND;
        }
        return Response.status(status).build();
    }

    @GET
    @Path("check-by-postgres")
    @Operation(
            summary = "Проверить паспорт (postgres)",
            description = "Полнотекстовый поиск посредством postgres (native)"
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
    public Response checkByPostgres(@Parameter(description = "Серия паспорта", required = true)
                                    @QueryParam("serial") String serial,
                                    @Parameter(description = "Номер паспорта", required = true)
                                    @QueryParam("number") String number) {
        Response.Status status = Response.Status.NOT_FOUND;
        if (service.checkByPostgres(serial.trim(), number.trim())) {
            status = Response.Status.FOUND;
        }
        return Response.status(status).build();
    }

    @GET
    @Path("check-by-elastic")
    @Operation(
            summary = "Проверить паспорт (elastic)",
            description = "Поиск посредством Hibernate + Elasticsearch"
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
    public Response checkByElastic(@Parameter(description = "Серия паспорта", required = true)
                                   @QueryParam("serial") String serial,
                                   @Parameter(description = "Номер паспорта", required = true)
                                   @QueryParam("number") String number) {
        Response.Status status = Response.Status.NOT_FOUND;
        if (service.checkByElastic(serial.trim(), number.trim())) {
            status = Response.Status.FOUND;
        }
        return Response.status(status).build();
    }
}
