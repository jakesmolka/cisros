package org.openehealth.ipf.tutorials.xds

import groovy.json.JsonOutput
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.json.JSONObject
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriUtils

/**
 * Processor to convert openEHR REST API request regarding deletion into valid ITI-57 request to update the objects availabilityStatus to deprecated.
 */
class OpenEhrToUpdateMetadataProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(OpenEhrDirectoryToProvideAndRegisterProcessor.class)

    /**
     * Processor to convert openEHR REST API request regarding deletion into valid ITI-57 request
     * @param exchange
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        //JSONObject json = new JSONObject(exchange.getIn().getBody(String.class))
        // TODO: this only works for DEL directory. for DEL composition this header has to be {preceding_version_uid}. so make check. rest should work for both.
        String id = "urn:uuid:"
        if (exchange.getIn().getHeader("If-Match") == null) {
            id += UriUtils.decode(exchange.getIn().getHeader("preceding_version_uid"), "UTF-8")
        } else {
            id += exchange.getIn().getHeader("If-Match").toString()
        }

        String ehrId = exchange.getIn().getHeader("ehr_id").toString()

        // retrieve patientId using given ehrId from request, because openEHR FOLDER has not patient ref itself
        URL url = new URL("http://localhost:8088/realserver/ehr/" + ehrId + "/ehr_status")
        String content = ContentUtils.httpGet(url)
        JSONObject ehrStatusJson = new JSONObject(content)
        String patientExtRef = ehrStatusJson.getJSONObject("subject").getJSONObject("external_ref").getJSONObject("id").getString("value")
        Identifiable patId = new Identifiable(patientExtRef, new AssigningAuthority("1.3"))

        // create submissionSet containing one UpdateAvailabilityStatus association targeting given id
        // using forFolder to let submissionSet be created with constant contentTypeCode, because delete request has no such information
        // and giving mocked json to pipe constant name and id of author for submissionSet's author attribute
        String author = "{\n" +
                "  \"feeder_audit\": {\n" +
                "    \"originating_system_audit\": {\n" +
                "      \"system_id\": \"2.999.2005.3.7\",\n" +
                "      \"provider\": {\n" +
                "        \"name\": \"Integration\",\n" +
                "        \"external_ref\": {\n" +
                "          \"id\": {\n" +
                "            \"value\": \"1\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}"
        JSONObject json = new JSONObject(author)
        SubmissionSet submissionSet = IntegrationUtils.createSubmissionSet(patId, json, true)

        Association delAssociation = IntegrationUtils.createAssociationUpdateAvailabilityStatus(submissionSet.getEntryUuid(), id, AvailabilityStatus.APPROVED, AvailabilityStatus.DEPRECATED)

        RegisterDocumentSet request = new RegisterDocumentSet()
        request.setSubmissionSet(submissionSet)
        request.getAssociations().add(delAssociation)

        exchange.getIn().setBody(request)
    }
}
