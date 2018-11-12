package org.openehealth.ipf.tutorials.xds

import org.apache.camel.Expression
import org.apache.camel.builder.RouteBuilder
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status

import static org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators.iti57RequestValidator

/**
 * Route Builder for ITI-57
 */
class Iti57RouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        errorHandler(noErrorHandler())

        //Note: implicitly reusing many "direct:..." endpoints from iti4142 routeBuilder

        // Entry point for Register Document Set
        from('xds-iti57:xds-iti57')
                .log(log) { 'received iti57: ' + it.in.getBody(RegisterDocumentSet.class) }
        // Validate and convert the request
                .process(iti57RequestValidator())
                .transform({ exchange, type ->
            ['req': exchange.in.getBody(RegisterDocumentSet.class), 'uuidMap': [:]] } as Expression
        )
        // Further validation based on the registry content
                .to('direct:checkForAssociationToDeprecatedObject', 'direct:checkPatientIds')
        // Store the individual entries contained in the request
                .multicast().to(
                'direct:storeSubmissionSet',
                'direct:storeAssociations')
                .end()
        // Create success response
                .transform(constant(new Response(Status.SUCCESS)))
    }

}
