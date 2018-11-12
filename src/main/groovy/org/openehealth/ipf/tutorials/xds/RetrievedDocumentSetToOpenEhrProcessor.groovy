package org.openehealth.ipf.tutorials.xds

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetrievedDocumentSetToOpenEhrProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(RetrievedDocumentSetToOpenEhrProcessor.class)

    // TODO: this surly needs to be changed to something more than concatenating later
    /**
     * Converts a RetrievedDocumentSet (i.e. response from ITI-43) into one concatenated string which will be written
     * into the exchange's body.
     * @param exchange
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        def retrievedDocumentSet = exchange.getIn().getBody() as RetrievedDocumentSet

        def body = ""
        retrievedDocumentSet.documents.each {body += it.dataHandler.content}

        exchange.getIn().setBody(body)
    }
}
