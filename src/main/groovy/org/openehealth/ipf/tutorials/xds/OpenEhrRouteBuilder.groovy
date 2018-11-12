package org.openehealth.ipf.tutorials.xds

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.util.toolbox.AggregationStrategies
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetRequestType
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti18RequestValidator
import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti18ResponseValidator
import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti41RequestValidator
import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti43RequestValidator
import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti57RequestValidator

/**
 * Route builder for openEHR REST API /composition and /directory endpoint.
 * (Based on openEHR REST API 0.9.4)
 */
class OpenEhrRouteBuilder extends RouteBuilder {
    private final static Logger log = LoggerFactory.getLogger(OpenEhrRouteBuilder.class)

    @Override
    public void configure() throws Exception {
        errorHandler(noErrorHandler())

        restConfiguration()
            .component("restlet")
            .host("localhost").port("8088")

        // Entry point for POSTing compositions
        rest().post("/ehr/{ehr_id}/composition")
            .to("direct:postComposition")

        from("direct:postComposition")
            .log(log) {"POST COMPOSITION:" + it.in.getBody(String.class)}
            .process(new OpenEhrCompositionToProvideAndRegisterProcessor())
            // convert to and validate if its now a correct request
            .convertBodyTo(ProvideAndRegisterDocumentSetRequestType.class)
            .process(iti41RequestValidator())  // debugging
            .log(log) { 'sending iti41: ' + it.in.getBody(ProvideAndRegisterDocumentSet.class) } // debugging
            // Forward to XDS web service
            .to('xds-iti41:localhost:9091/xds-iti41')
            // Create success response
            .transform ( constant(new Response(Status.SUCCESS)) )

        // Entry point for POSTing directory
        rest().post("/ehr/{ehr_id}/directory")
                .to("direct:postDirectory")

        from("direct:postDirectory")
            .log(log) {"POST DIRECTORY:" + it.in.getBody(String.class)}
            .to('direct:mockPostDirectory')  // TODO: post-prototype: replace with processor executing real POST openEHR CDR
            // to only forward copy of exchange, to keep above's POST response
            .enrich("direct:processPostDirectory", AggregationStrategies.useOriginal())
            // Create success response including intermediate POST response from openEHR CDR
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))

        from('direct:processPostDirectory')
            .process(new OpenEhrDirectoryToProvideAndRegisterProcessor())
            // convert to and validate if its now a correct request
            .convertBodyTo(ProvideAndRegisterDocumentSetRequestType.class)
            .process(iti41RequestValidator())  // debugging
            .log(log) { 'sending iti41: ' + it.in.getBody(ProvideAndRegisterDocumentSet.class) } // debugging
            // Forward to XDS web service
            .to('xds-iti41:localhost:9091/xds-iti41')

        // Entry point for PUTing composition
        rest().put("/ehr/{ehr_id}/composition/{preceding_version_uid}")
            .to("direct:putComposition")

        from("direct:putComposition")
            .log(log) {"PUT COMPOSITION"}
            .to("direct:postComposition")  // reuse this endpoint with added check for put

        // Entry point for PUTing directory
        rest().put("/ehr/{ehr_id}/directory")
            .to("direct:putDirectory")

        from("direct:putDirectory")   // can use and combine existing endpoints
            .log(log) {"PUT DIRECTORY"}
            .multicast().to(
                "direct:deleteRequest",
                "direct:postDirectory"
            )

        // Entry point for DELETE of composition
        rest().delete("/ehr/{ehr_id}/composition/{preceding_version_uid}")
            .to("direct:deleteRequest")

        // Entry point for DELETE of directory
        // has header: If-Match: {preceding_version_uid}
        rest().delete("/ehr/{ehr_id}/directory")
            .to("direct:deleteRequest")

        from("direct:deleteRequest")
            .log(log) {"DELETE"}
            .process(new OpenEhrToUpdateMetadataProcessor())
            // convert to and validate if its now a correct request
            .log(log) { 'DELETE before convert: ' + it.in.getBody(String.class) } // debugging
            .convertBodyTo(SubmitObjectsRequest.class)
            .log(log) { 'DELETE before validation: ' + it.in.getBody(String.class) } // debugging
            .process(iti57RequestValidator())  // debugging
            .log(log) { 'sending iti57: ' + it.in.getBody(RegisterDocumentSet.class) } // debugging
            // Forward to XDS web service
            .to('xds-iti57:localhost:9091/xds-iti57')
            // Create success response
            .transform ( constant(new Response(Status.SUCCESS)) )

        // Entry point for GETing compositions
        // TODO: GETing should be native not integrated to use XDS. ONLY FOR PROTOTYPE TESTING
        rest().get("/ehr/{ehr_id}/composition/{version_uid}")
                .to("direct:getComposition")

        from("direct:getComposition")
            .process(new OpenEhrToRegistryStoredQueryProcessor())
            .convertBodyTo(AdhocQueryRequest.class)
            .process(iti18RequestValidator()) // debugging
            .to('xds-iti18:localhost:9091/xds-iti18')
            .process(iti18ResponseValidator()) // debugging
            // convert ITI-18 response of type AdhocQueryResponse (ebXML) to simplified model
            .convertBodyTo(QueryResponse.class)
            .process(new QueryResponseToRetrieveDocumentSetProcessor())
            // convert to ebXML representation and forward to retrieval
            .convertBodyTo(RetrieveDocumentSetRequestType.class)
            .process(iti43RequestValidator())
            .to('xds-iti43:localhost:9091/xds-iti43')
            // convert ITI-43 response of type RetrieveDocumentSetResponseType to simplified model
            .convertBodyTo(RetrievedDocumentSet.class)
            // finally extract and return payload
            .process(new RetrievedDocumentSetToOpenEhrProcessor())
            // return body as REST response
            .transform().body()
    }
}
