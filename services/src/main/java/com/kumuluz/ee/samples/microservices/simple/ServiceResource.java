package com.kumuluz.ee.samples.microservices.simple;

import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.Logger;
import com.kumuluz.ee.samples.microservices.simple.models.Service;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/services")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceResource {

    private static final Logger LOG = LogManager.getLogger(ServiceResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    /**
     * Vrne seznam vseh servicev
     * */


    @Inject
    @Metric(name = "histogram_dodanih")
    Histogram histogram;

    @GET
    @Metered(name = "getServices_meter")
    public Response getServices() {
        LOG.trace("getServices ENTRY");
        TypedQuery<Service> query = em.createNamedQuery("Service.findAll", Service.class);

        List<Service> services = query.getResultList();
        histogram.update(services.size());
        LOG.info("Stevilo dodanih storitev: {}", services.size());
        return Response.ok(services).build();
    }
    /**
     * Pridobi posamezen service glede na njegov id
     */


    @GET
    @Path("/{id}")
    @Timed(name = "getService_timer")
    public Response getService(@PathParam("id") Integer id) {
        LOG.trace("getService ENTRY");
        Service p = em.find(Service.class, id);

        if (p == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        LOG.info("Prikazana storitev ID: {}", p.getId());
        return Response.ok(p).build();
    }

    /**
     * Omogoča urejanje servica  tako, da staremu servicu nastavi nove vrednosti
     */
    @POST
    @Path("/{id}")
    public Response editService(@PathParam("id") Integer id, Service service) {
        LOG.trace("editService ENTRY");
        Service p = em.find(Service.class, id);

        if (p == null)
            return Response.status(Response.Status.NOT_FOUND).build();

       p.setTitle(service.getTitle());
       p.setDescription(service.getDescription());
       p.setLocation(service.getLocation());
       p.setPrice(service.getPrice());

        em.getTransaction().begin();

        em.persist(p);

        em.getTransaction().commit();
        LOG.info("Urejena storitev ID: {}", p.getId());
        return Response.status(Response.Status.CREATED).entity(p).build();
    }

    /**
     * Doda nov service (Service p)
     */
    @POST
    public Response createService(Service p) {
        LOG.trace("createService ENTRY");
        p.setId(null);

        em.getTransaction().begin();

        em.persist(p);

        em.getTransaction().commit();
        LOG.info("Create service ID: {}", p.getId());
        return Response.status(Response.Status.CREATED).entity(p).build();
    }

    /**
     * Vrne config info
     * */

    @Inject
    private ServiceProperties properties;

    @GET
    @Path("/config")
    public Response test() {
        LOG.trace("config ENTRY");
        String response =
                "{" +
                        "\"jndi-name\": \"%s\"," +
                        "\"connection-url\": \"%s\"," +
                        "\"username\": \"%s\"," +
                        "\"password\": \"%s\"," +
                        "\"max-pool-size\": %d" +
                        "}";

        response = String.format(
                response,
                properties.getJndiName(),
                properties.getConnectionUrl(),
                properties.getUsername(),
                properties.getPassword(),
                properties.getMaxPoolSize()
        );
        LOG.trace("config uspesen EXIT");
        return Response.ok(response).build();
    }
}
