package org.openehealth.ipf.tutorials.xds

import groovy.json.JsonSlurper
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.json.JSONObject
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Folder
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet

/**
 * Processor to convert openEHR REST API request regarding DIRECTORY into valid ITI-41 request
 * TODO: Static sample test data only right now!
 */
class OpenEhrDirectoryToProvideAndRegisterProcessor implements Processor{
    private final static Logger log = LoggerFactory.getLogger(OpenEhrDirectoryToProvideAndRegisterProcessor.class)

    /**
     * Processor to create ProvideAndRegisterDocumentSet from openEHR request to create Directory
     * @param exchange
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        String payload = exchange.getIn().getBody(String.class)
        JSONObject json = new JSONObject(payload)
        String ehrId = exchange.getIn().getHeader("ehr_id").toString()
        def request = generateProvideAndRegisterDocumentSet(json, ehrId)

        exchange.getIn().setBody(request)
    }

    /**
     * Helper function to invoke the actual processing
     * @param createdDirJson
     * @param ehrId
     * @return
     */
    private ProvideAndRegisterDocumentSet generateProvideAndRegisterDocumentSet(JSONObject createdDirJson, String ehrId) {
        // openEHR folder can have sub-folders, but at least the most super-class needs to be submitted
        // and unfortunately the first one need to be created out of loop because patientId is necessary for submissionSet
        Folder folder = IntegrationUtils.createFolder(ehrId, createdDirJson, createdDirJson.getJSONObject("name").getString("value"))

        SubmissionSet submissionSet = IntegrationUtils.createSubmissionSet(folder.getPatientId(), createdDirJson, true)

        Association folderAssociation = IntegrationUtils.createAssociationFolderToSubmissionSet(submissionSet.getEntryUuid(), folder.getEntryUuid())

        ProvideAndRegisterDocumentSet request = new ProvideAndRegisterDocumentSet()
        request.setSubmissionSet(submissionSet)
        request.getFolders().add(folder)
        request.getAssociations().add(folderAssociation)

        // extract docEntry entryUuid from DIRECTORY request and invoke necessary action for each referenced document (item)
        createdDirJson.getJSONArray("items").each {
            Association docFolderAssociation = IntegrationUtils.createAssociationDocEntryToFolder(it.getString("uid"), folder.getEntryUuid())
            request.getAssociations().add(docFolderAssociation)
        }

        // if there are sub-folders add them with coded titles too - given this folders title
        if (createdDirJson.has("folders")) {
            createdDirJson.getJSONArray("folders").each {
                addFolder(request, it, ehrId, folder.getTitle().value)
            }
        }

        request.setTargetHomeCommunityId("urn:oid:1.2.3.4.5.6.2333.23")
        request
    }

    /**
     * Helper function to recursively create sub-folders
     * @param request
     * @param json
     * @param ehrId
     * @param superName
     */
    private void addFolder(ProvideAndRegisterDocumentSet request, JSONObject json, String ehrId, String superName){
        // sub-folder gets coded name from super-folder and its own, divided by "/"
        // also, in turn, each folder gets POSTed by createFolder to openEHR CDR via REST API
        Folder folder = IntegrationUtils.createFolder(ehrId, json, superName + "/" + json.getJSONObject("name").getString("value"))

        Association folderAssociation = IntegrationUtils.createAssociationFolderToSubmissionSet(request.getSubmissionSet().getEntryUuid(), folder.getEntryUuid())

        request.getFolders().add(folder)
        request.getAssociations().add(folderAssociation)

        // extract docEntry entryUuid from DIRECTORY request and invoke necessary action for each referenced document (item)
        json.getJSONArray("items").each {
            Association docFolderAssociation = IntegrationUtils.createAssociationDocEntryToFolder(it.getString("uid"), folder.getEntryUuid())
            request.getAssociations().add(docFolderAssociation)
        }
    }


}
