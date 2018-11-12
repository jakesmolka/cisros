package org.openehealth.ipf.tutorials.xds

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Route builder to mock openEHR server features that would be available in a real, non-prototype scenario.
 * Can return mocked ehr_status to allow retrieval of external patient ID.
 * Also mocks the actual submission of folders so their unique ID can be used.
 */
class OpenEhrServerMockRouteBuilder extends RouteBuilder{
    private final static Logger log = LoggerFactory.getLogger(OpenEhrRouteBuilder.class)

    @Override
    public void configure() throws Exception {
        errorHandler(noErrorHandler())

        // Allow retrieval of details about EHR (EHR_STATUS)
        rest().get("realserver/ehr/{ehr_id}/ehr_status")
            .to("direct:mockEhrStatus")

        from("direct:mockEhrStatus")
            .transform().constant("{\n" +
                "  \"uid\": \"...\",\n" +
                "  \"archetype_node_id\": \"\",\n" +
                "  \"name\": \"\",\n" +
                "  \"archetype_details\": {\n" +
                "    \"archetype_id\": \"...\",\n" +
                "    \"rm_version\": \"1.0.3\"\n" +
                "  },\n" +
                "  \"subject\": {\n" +
                "    \"external_ref\": {\n" +
                "      \"namespace\": \"DEMOGRAPHIC\",\n" +
                "      \"type\": \"PERSON\",\n" +
                "      \"id\": {\n" +
                "        \"_type\": \"HIER_OBJECT_ID\",\n" +
                "        \"value\": \"_valid_root_uid_::_extension_\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"is_queryable\": true,\n" +
                "  \"is_modifiable\": true,\n" +
                "  \"other_details\": {\n" +
                "    \"_type\": \"ITEM_TREE\",\n" +
                "    \"items\": {}\n" +
                "  }\n" +
                "}")

        // Allow posting directory with getting a mocked uniqueId as response
        rest().post("realserver/ehr/{ehr_id}/directory")
            .to("direct:mockPostDirectory")

        // mocking successful creation of directory, incl. returning of uid depending on given ehrId and folder name
        from("direct:mockPostDirectory")
            .process(new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                // generating uid depending on input so same uid is given, which is necessary for tests
                def json = new JsonSlurper().parseText(exchange.getIn().getBody(String.class))
                // handle super-folder
                String ehrId = exchange.getIn().getHeader("ehr_id").toString()
                String seed = json.name.value + ehrId
                String uid = UUID.nameUUIDFromBytes(seed.getBytes()).toString() + "::example.domain.com::1"
                json << [uid: uid]   // add to json lazyList structure

                // handle subfolders with recursive helper
                if (json.folders instanceof List && json.folders.size() > 0){
                    json.folders.each() {
                        processDeep(it, ehrId)
                    }
                }

                exchange.getIn().setBody(JsonOutput.toJson(json))
            }})
    }

    def processDeep(Map folder, String ehrId){
        String seed = folder.name.value + ehrId
        String uid = UUID.nameUUIDFromBytes(seed.getBytes()).toString() + "::example.domain.com::1"
        folder << [uid: uid]   // add to json lazyList structure

        if (folder.folders instanceof List && folder.folders.size() > 0){
            folder.folders.each() {
                processDeep(it, ehrId)
            }
        }
    }
}
