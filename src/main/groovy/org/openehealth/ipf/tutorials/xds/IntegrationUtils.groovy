package org.openehealth.ipf.tutorials.xds

import ca.uhn.hl7v2.model.v281.datatype.DTM
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationLabel
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Folder
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet
import groovy.json.*

import java.text.ParseException

/**
 * Utilities to generate XDS metadata objects and structures by given openEHR REST input.
 */
abstract class IntegrationUtils {
    // parse config file located at resources folder
    static def config = new JsonSlurper().parseText(IOUtils.toString(this.getClass().getResource("/integration_config.json"), "UTF-8"))

    private IntegrationUtils() {
        throw new UnsupportedOperationException('Cannot be instantiated')
    }

    /**
     * Create association to link DocumentEntry to Folder
     * @param docEntryEntryUuid
     * @param folderEntryUuid
     * @return
     */
    static Association createAssociationDocEntryToFolder(String docEntryEntryUuid, String folderEntryUuid) {
        Association docFolderAssociation = new Association()
        docFolderAssociation.setAssociationType(AssociationType.HAS_MEMBER)
        // source folder
        docFolderAssociation.setSourceUuid(folderEntryUuid)
        // targeted document
        docFolderAssociation.setTargetUuid(docEntryEntryUuid)
        UUID uuid = UUID.randomUUID()
        docFolderAssociation.setEntryUuid(uuid.toString())
        docFolderAssociation
    }

    /**
     * Create association to link Folder to SubmissionSet
     * @param SubmissionSetEntryUuid
     * @param folderEntryUuid
     * @return
     */
    static Association createAssociationFolderToSubmissionSet(String SubmissionSetEntryUuid, String folderEntryUuid) {
        Association folderAssociation = new Association()
        folderAssociation.setAssociationType(AssociationType.HAS_MEMBER)
        folderAssociation.setSourceUuid(SubmissionSetEntryUuid)
        folderAssociation.setTargetUuid(folderEntryUuid)
        UUID uuid = UUID.randomUUID()
        folderAssociation.setEntryUuid(uuid.toString())
        folderAssociation
    }

    /**
     * Create association to link DocumentEntry to SubmissionSet
     * @param docEntryEntryUuid
     * @param submissionSetEntryUuid
     * @return
     */
    static Association createAssociationDocEntryToSubmissionSet(String docEntryEntryUuid, String submissionSetEntryUuid) {
        Association docAssociation = new Association();
        docAssociation.setAssociationType(AssociationType.HAS_MEMBER);
        // source submissionSet
        docAssociation.setSourceUuid(submissionSetEntryUuid)
        // targeted document
        docAssociation.setTargetUuid(docEntryEntryUuid);
        docAssociation.setLabel(AssociationLabel.ORIGINAL);
        UUID uuid = UUID.randomUUID()
        docAssociation.setEntryUuid(uuid.toString())
        docAssociation.setPreviousVersion("111")
        docAssociation
    }

    /**
     * Create association to update the availabilityStatus attribute
     * @param source
     * @param target
     * @param oldStatus
     * @param newStatus
     * @return
     */
    static  Association createAssociationUpdateAvailabilityStatus(String source, String target, AvailabilityStatus oldStatus, AvailabilityStatus newStatus){
        Association asso = new Association()
        asso.setAssociationType(AssociationType.UPDATE_AVAILABILITY_STATUS)
        asso.setSourceUuid(source)
        asso.setTargetUuid(target)
        asso.setOriginalStatus(oldStatus)
        asso.setNewStatus(newStatus)
        UUID uuid = UUID.randomUUID()
        asso.setEntryUuid(uuid.toString())
        asso
    }

    /**
     * Create association to replace a documentEntry
     * @param source - "sourceObject attribute of the Association object shall be the entryUUID of the new DocumentEntry contained in the SubmissionSet."
     * @param target - therefore the old, to be replaced documentEntry ID
     * @return
     */
    static  Association createAssociationReplace(String source, String target){
        Association asso = new Association()
        asso.setAssociationType(AssociationType.REPLACE)
        asso.setSourceUuid(source)
        asso.setTargetUuid(target)
        UUID uuid = UUID.randomUUID()
        asso.setEntryUuid(uuid.toString())
        asso
    }

    /**
     * Create a XDS Folder object by given openEHR input
     * @param ehrId - Connection to patient needs to be provided through linked openEHR EHR
     * @param json - Input data
     * @param title - Given to allow injecting coded titles for sub-folders
     * @return
     */
    static Folder createFolder(String ehrId, JSONObject json, String title) {
        Folder folder = new Folder()

        // set initial status (D2)
        folder.setAvailabilityStatus(AvailabilityStatus.APPROVED)

        // to allow all plain folders: check if new "other_context" attribute is available and give constant otherwise (D4)
        if (json.getJSONObject("context").has("other_context")) {
            // set codeList from DIRECTORY input and set fix scheme, according to German XDS value set
            json.getJSONObject("context").getJSONObject("other_context").getJSONArray("items").each {
                if (it.getJSONObject("name").getString("value").equals("codeList")) {
                    String code = it.getJSONObject("value").getString("value")
                    folder.getCodeList().add(new Code(code, new LocalizedString(code), "1.3.6.1.4.1.19376.3.276.1.5.7"))
                }
            }
        } else {
            folder.getCodeList().add(new Code("0", new LocalizedString("n.a."), "1.3.6.1.4.1.19376.3.276.1.5.7"))
        }
        if (folder.getCodeList().size() < 1 ) {
            throw new ParseException("context.other_context.items[].name == contentTypeCode not existing or wrong format", 1)
        }

        // set entryUuid, which was returned by openEHR server as response in previous step, as value of uniqueId (D8)
        String uid = json.getString("uid")
        folder.setEntryUuid("urn:uuid:" + uid)
        if (folder.getEntryUuid() == null ) {
            throw new ParseException("created directory: uid not existing or wrong format", 1)
        }

        // set patientId to given value (D15)
        // retrieve patientId using given ehrId from request, because openEHR FOLDER has not patient ref itself
        URL url = new URL("http://localhost:8088/realserver/ehr/" + ehrId + "/ehr_status")
        String content = ContentUtils.httpGet(url)
        JSONObject ehrStatusJson = new JSONObject(content)
        String patientExtRef = ehrStatusJson.getJSONObject("subject").getJSONObject("external_ref").getJSONObject("id").getString("value")
        Identifiable patId = new Identifiable(patientExtRef, new AssigningAuthority("1.3"))
        folder.setPatientId(patId)
        if (folder.getPatientId() == null) {
            throw new ParseException("subject.external_ref.id not existing or wrong format", 1)
        }

        // set title - can be coded with "/" to include openEHR sub-folders (D24)
        folder.setTitle(new LocalizedString(title, "en-US", "UTF8"))

        // set entryUuid, which was returned by openEHR server as response, as value of uniqueId (D26)
        folder.setUniqueId("urn:uuid:" + uid)
        folder
    }

    /**
     * Create a XDS DocumentEntry object by given openEHR input
     * @param json - Input data
     * @param ehrId Connection to patient needs to be provided through linked openEHR EHR
     * @return
     */
    static DocumentEntry createDocumentEntry(JSONObject json, String ehrId) {
        // extract templateId for later usage
        String template = json.getJSONObject("archetype_details").getJSONObject("template_id").getString("value")

        // get Author from composition (D1)
        Author author = new Author()
        String authorName = json.getJSONObject("composer").getString("name")
        String authorId = json.getJSONObject("composer").getJSONObject("external_ref").getJSONObject("id").getString("value")
        author.getAuthorInstitution().add(new Organization(authorName, "urn:uuid:" + authorId, new AssigningAuthority("1.3"))) //TODO: what to do with assigning auth?
        if (author.getAuthorInstitution().isEmpty()) {
            throw new ParseException("composer.name or composer.external_ref.id not existing or wrong format", 1)
        }

        DocumentEntry docEntry = new DocumentEntry()
        docEntry.getAuthors().add(author)

        // set initial status (D2)
        docEntry.setAvailabilityStatus(AvailabilityStatus.APPROVED)

        // read from config which classCode to assign according to given archetype (scheme according to German XDS value set) (D3)
        if (config instanceof Map) {
            config.classCode.each {
                // access each configured classCode
                if (it.template instanceof String) {
                    if (it.template == template) {
                        // then take given config value as code
                        docEntry.setClassCode(new Code(it.class.code, new LocalizedString(it.class.localized), "1.3.6.1.4.1.19376.3.276.1.5.8"))
                    }
                }
            }
        }
        if (docEntry.getClassCode() == null ) {
            throw new ParseException("config for classCode not existing or wrong format", 1)
        }

        // read from config which confidentialityCode to assign according to given archetype (scheme according to German XDS value set) (D5)
        if (config instanceof Map) {
            config.confidentialityCode.each {
                // access each configured confidentialityCode
                if (it.template instanceof String) {
                    if (it.template == template) {
                        // then take given config value as code
                        docEntry.getConfidentialityCodes().add(new Code(it.confidentiality.code, new LocalizedString(it.confidentiality.localized), "2.16.840.1.113883.5.25"))
                    }
                }
            }
        }
        if (docEntry.getConfidentialityCodes().size() < 1) {
            throw new ParseException("config missing or information about archetype not existing or wrong format", 1)
        }

        // set creationTime to time given by originating system audit in composition (D7)
        String originatingTime = json.getJSONObject("feeder_audit").getJSONObject("originating_system_audit").getString("time")
        docEntry.setCreationTime(ContentUtils.openEhrTimeToHl7Time(originatingTime))
        if (docEntry.getCreationTime() == null) {
            throw new ParseException("feeder_audit.originating_system_audit.time not existing or wrong format", 1)
        }

        // get entryUuid from composition (D8)
        docEntry.setEntryUuid("urn:uuid:" + json.getJSONObject("uid").getString("value"))
        if (docEntry.getEntryUuid() == null) {
            throw new ParseException("uid not existing or wrong format", 1)
        }

        // set to fixed value and scheme according to international XDS value set (D9)
        docEntry.setFormatCode(new Code("urn:ihe:domain:openehr:composition:1.0.2", new LocalizedString("openEHR composition"), "1.3.6.1.4.1.19376.1.2.3"))

        // hash (D10) needs to be set with access to the actual document data (i.e. function that calls this one)

        // read from config which healthCareFacilityTypeCode to assign according to given originating system id (scheme according to German XDS value set) (D11)
        String originatingSystemId = json.getJSONObject("feeder_audit").getJSONObject("originating_system_audit").getString("system_id")
        if (config instanceof Map) {
            config.healthCareFacilityTypeCode.each {
                // access each configured confidentialityCode
                if (it.systemId instanceof String) {
                    if (it.systemId == originatingSystemId) {
                        // then take given config value as code
                        docEntry.setHealthcareFacilityTypeCode(new Code(it.healthCareFacilityType.code, new LocalizedString(it.healthCareFacilityType.localized), "1.3.6.1.4.1.19376.3.276.1.5.2"))
                    }
                }
            }
        }
        if (docEntry.getHealthcareFacilityTypeCode() == null) {
            throw new ParseException("context.other_context.items[].name == healthCareFacilityTypeCode not existing or wrong format", 1)
        }

        // get languageCode from composition (D12)
        docEntry.setLanguageCode(json.getJSONObject("language").getString("code_string"))
        if (docEntry.getLanguageCode() == null) {
            throw new ParseException("language.code_string not existing or wrong format", 1)
        }

        // set fixed mimeType (D13)
        docEntry.setMimeType("application/json")

        // static Stable type (D14)
        docEntry.setType(DocumentEntryType.STABLE)

        // get external patient id (i.e. XDSs, read through openEHR) (D15)
        URL url = new URL("http://localhost:8088/realserver/ehr/" + ehrId + "/ehr_status")
        String content = ContentUtils.httpGet(url)
        JSONObject ehrStatusJson = new JSONObject(content)
        String patientExtRef = ehrStatusJson.getJSONObject("subject").getJSONObject("external_ref").getJSONObject("id").getString("value")
        docEntry.setPatientId(new Identifiable(patientExtRef, new AssigningAuthority("1.3")))  // TODO: what to do with assigning auth?
        if (docEntry.getPatientId() == null) {
            throw new ParseException("subject.external_ref.id not existing or wrong format", 1)
        }

        // read from config which practiceSettingCode to assign according to given archetype (scheme according to German XDS value set) (D16)
        String location = json.getJSONObject("context").getString("location")
        if (config instanceof Map) {
            config.practiceSettingCode.each {
                // access each configured confidentialityCode
                if (it.location instanceof String) {
                    if (it.location == location) {
                        // then take given config value as code
                        docEntry.setPracticeSettingCode(new Code(it.practiceSetting.code, new LocalizedString(it.practiceSetting.localized), "2.16.840.1.113883.5.25"))
                    }
                }
            }
        }
        if (docEntry.getPracticeSettingCode() == null) {
            throw new ParseException("config of practiceSettingCode not existing or wrong format", 1)
        }

        // repositoryId (D17) gets set from Repository itself later

        // get serviceStartTime from composition (D18)
        String originalStartTime = json.getJSONObject("context").getJSONObject("start_time").getString("value")
        docEntry.setServiceStartTime(ContentUtils.openEhrTimeToHl7Time(originalStartTime))
        if (docEntry.getServiceStartTime() == null) {
            throw new ParseException("context.start_time not existing or wrong format", 1)
        }

        // get serviceStopTime from composition when not empty (D19)
        if (json.getJSONObject("context").has("end_time")) {
            String originalEndTime = json.getJSONObject("context").getJSONObject("end_time").getString("value")
            docEntry.setServiceStopTime(ContentUtils.openEhrTimeToHl7Time(originalEndTime))
            if (docEntry.getServiceStopTime() == null) {
                throw new ParseException("context.end_time existing but empty or wrong format", 1)
            }
        } // can stay null if input way non existent too

        // size (D20) needs to be set with access to the actual document data (i.e. function that calls this one)

        // set source patient ID to openEHR's ehrId, since patientId is already universal patient ID (D22)
        docEntry.setSourcePatientId(new Identifiable(ehrId, new AssigningAuthority("4.1"))) // TODO: what to do with assigning auth?

        // set title to name of composition (D24)
        if (json.has("name")) {
            docEntry.setTitle(new LocalizedString(json.getJSONObject("name").getString("value")))
        }
        if (docEntry.getTitle() == null) {
            throw new ParseException("name.value not existing or wrong format", 1)
        }

        // read from config which typeCode to assign according to given archetype (scheme according to German XDS value set) (D25)
        if (config instanceof Map) {
            config.typeCode.each {
                // access each configuration
                if (it.template instanceof String) {
                    if (it.template == template) {
                        // then take given config value as code
                        docEntry.setTypeCode(new Code(it.type.code, new LocalizedString(it.type.localized), "1.3.6.1.4.1.19376.3.276.1.5.9"))
                    }
                }
            }
        }
        if (docEntry.getTypeCode() == null) {
            throw new ParseException("config for typeCode not existing or wrong format", 1)
        }

        // set new random uniqueId (D26)
        UUID uuid = UUID.randomUUID()
        docEntry.setUniqueId("urn:uuid:" + uuid.toString())

        docEntry
    }

    /**
     * Create XDS SubmissionSet object as envelope for transaction
     * @param patientID - Identification of patient needs to be provided
     * @param json - Input data
     * @param forFolder - Switch to control behavior in case of action regarding Folder
     * @return
     */
    static SubmissionSet createSubmissionSet(Identifiable patientID, JSONObject json, boolean forFolder) {
        // get Author from composition (D1)
        Author author = new Author()
        String authorName = json.getJSONObject("feeder_audit").getJSONObject("originating_system_audit").getJSONObject("provider").getString("name")
        String authorId = json.getJSONObject("feeder_audit").getJSONObject("originating_system_audit").getJSONObject("provider")
                .getJSONObject("external_ref").getJSONObject("id").getString("value")
        author.getAuthorInstitution().add(new Organization(authorName, "urn:uuid:" + authorId, new AssigningAuthority("1.3"))) //TODO: what to do with assigning auth?
        if (author.getAuthorInstitution().isEmpty()) {
            throw new ParseException("feeder_audit.originating_system_audit.provider.name or ...provider.external_ref.id not existing or wrong format", 1)
        }

        SubmissionSet submissionSet = new SubmissionSet()
        submissionSet.getAuthors().add(author)

        // read from config which contentTypeCode to assign according to given archetype (scheme according to German XDS value set) (D6)
        if (forFolder) {    // if submissionSet for Folder -> add fixed code because no template is available
            submissionSet.setContentTypeCode(new Code("12", new LocalizedString("Integration"), "1.3.6.1.4.1.19376.3.276.1.5.12"))
        } else {            // if submissionSet for DocumentEntry
            String template = json.getJSONObject("archetype_details").getJSONObject("template_id").getString("value")
            if (config instanceof Map) {
                config.contentTypeCode.each {
                    // access each configuration
                    if (it.template instanceof String) {
                        if (it.template == template) {
                            // then take given config value as code
                            submissionSet.setContentTypeCode(new Code(it.type.code, new LocalizedString(it.type.localized), "1.3.6.1.4.1.19376.3.276.1.5.12"))
                        }
                    }
                }
            }
        }
        if (submissionSet.getContentTypeCode() == null) {
            throw new ParseException("config for contentTypeCode not existing or wrong format", 1)
        }

        // set new random uniqueId (D8)
        UUID uuid = UUID.randomUUID()
        submissionSet.setEntryUuid("urn:uuid:" + uuid.toString());

        // set patientId to given value (D15)
        submissionSet.setPatientId(patientID);

        // sets sourceId by parsing submitted FOLDER (D21)
        String id = json.getJSONObject("feeder_audit").getJSONObject("originating_system_audit").getString("system_id")
        submissionSet.setSourceId(id)
        if (submissionSet.getSourceId() == null) {
            throw new ParseException("feeder_audit.originating_system_audit.system_id not existing or wrong format", 1)
        }

        // current system time in HL7 DTM (D23)
        Calendar now = Calendar.getInstance()
        DTM dtm = new DTM()
        dtm.setValue(now)
        submissionSet.setSubmissionTime(dtm.toString())

        // set new random uniqueId (D26)
        uuid = UUID.randomUUID()
        submissionSet.setUniqueId("urn:uuid:" + uuid.toString());

        submissionSet.setHomeCommunityId("urn:oid:1.2.3.4.5.6.2333.23");
        submissionSet
    }
}
