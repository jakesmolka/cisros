package org.openehealth.ipf.tutorials.xds

import groovy.json.JsonSlurper
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * TODO: ONLY FOR PROTOTYPE TESTING
 *
 * Processor to convert openEHR REST API request into valid ITI-18 request.
 */
public class OpenEhrToRegistryStoredQueryProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(OpenEhrToRegistryStoredQueryProcessor.class)

    public void process(Exchange exchange) throws Exception {
        // extract data from request
        String compositionPayload = exchange.getIn().getBody(String.class)
        def json = new JsonSlurper().parseText(compositionPayload)
        def id = "urn:uuid:" + json.uid.value

        // create query object
        def query = new GetDocumentsQuery()
        query.getUniqueIds().add(id)
        def queryRegistry = new QueryRegistry(query)

        exchange.getIn().setBody(queryRegistry)
    }
}
