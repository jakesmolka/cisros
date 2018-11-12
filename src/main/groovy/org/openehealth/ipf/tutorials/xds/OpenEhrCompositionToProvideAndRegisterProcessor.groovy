package org.openehealth.ipf.tutorials.xds

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Document
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriUtils

import javax.activation.DataHandler
import org.json.*

/**
 * Processor to convert openEHR REST API request regarding COMPOSITION into valid ITI-41 request.
 * TODO: Static sample test data only right now!
 */
public class OpenEhrCompositionToProvideAndRegisterProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(OpenEhrCompositionToProvideAndRegisterProcessor.class)

    public void process(Exchange exchange) throws Exception {
        // add document with input from POST as payload
        String compositionPayload = exchange.getIn().getBody(String.class)
        JSONObject json = new JSONObject(compositionPayload)
        def dataHandler = new DataHandler(compositionPayload, "text/plain")  // TODO: refactor to "application/json"
        String ehrId = exchange.getIn().getHeader("ehr_id").toString()
        // if the request came from PUT, processing is almost like POST but with little modification regarding versionId and replace (RPLC) association
        String precedingVersion = exchange.getIn().getHeader("preceding_version_uid")
        String putId = null
        if (precedingVersion != null) {
            putId = UriUtils.decode(precedingVersion, "UTF-8")
        }
        def request = generateProvideAndRegisterDocumentSet(dataHandler, json, ehrId, putId)

        exchange.getIn().setBody(request)
    }

    /**
     * Creates ITI-41 request.
     *
     * @return New and enriched {@Link ProvideAndRegisterDocumentSet}
     */
    private ProvideAndRegisterDocumentSet generateProvideAndRegisterDocumentSet(DataHandler dataHandler, JSONObject json, String ehrId, String putId) {
        DocumentEntry docEntry = IntegrationUtils.createDocumentEntry(json, ehrId)
        SubmissionSet submissionSet = IntegrationUtils.createSubmissionSet(docEntry.getPatientId(), json, false)

        Association docAssociation
        if (putId != null) {
            // then a PUT request was send. invoke replace (RPLC)
            docAssociation = IntegrationUtils.createAssociationReplace(docEntry.getEntryUuid(), "urn:uuid:" + putId)
        } else {
            docAssociation = IntegrationUtils.createAssociationDocEntryToSubmissionSet(docEntry.getEntryUuid(), submissionSet.getEntryUuid())
        }

        // create document with given payload
        Document doc = new Document(docEntry, dataHandler)
        // overwrite hash and size (D10 & D20) - TODO: need to use openEHR's canonicalForm() when its available
        docEntry.hash = ContentUtils.sha1(dataHandler)
        docEntry.size = ContentUtils.size(dataHandler)

        ProvideAndRegisterDocumentSet request = new ProvideAndRegisterDocumentSet()
        request.setSubmissionSet(submissionSet)
        request.getDocuments().add(doc)
        request.getAssociations().add(docAssociation)

        request.setTargetHomeCommunityId("urn:oid:1.2.3.4.5.6.2333.23")
        request
    }
}
