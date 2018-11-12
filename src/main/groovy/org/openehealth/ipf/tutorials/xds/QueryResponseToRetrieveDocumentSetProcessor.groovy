package org.openehealth.ipf.tutorials.xds

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.openehealth.ipf.commons.ihe.xds.core.requests.DocumentReference
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Processor to convert ITI-18 query response into valid ITI-43 request.
 * TODO: Static sample test data only right now!
 */
public class QueryResponseToRetrieveDocumentSetProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(QueryResponseToRetrieveDocumentSetProcessor.class)

    public void process(Exchange exchange) throws Exception {
        // TODO: get data from QueryResponse (aka ids) and generate RetrieveDocumentSet
        // TODO: static test right now
        def retrieveDocSet = new RetrieveDocumentSet()
        def doc = new DocumentReference()
        doc.setDocumentUniqueId("4.3.2.1")
        doc.setRepositoryUniqueId("something")
        retrieveDocSet.documents.add(doc)

        exchange.getIn().setBody(retrieveDocSet)
    }
}
