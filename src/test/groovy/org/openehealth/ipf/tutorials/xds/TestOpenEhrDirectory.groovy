package org.openehealth.ipf.tutorials.xds

import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.Response
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.apache.cxf.transport.servlet.CXFServlet
import org.junit.BeforeClass
import org.junit.Test
import org.openehealth.ipf.commons.core.config.ContextFacade
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Folder
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindFoldersQuery
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status
import org.openehealth.ipf.platform.camel.ihe.ws.StandardTestContainer

import static com.jayway.restassured.RestAssured.given
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertNotNull

/**
 * Tests against openEHR REST API /directory endpoint.
 */
class TestOpenEhrDirectory extends StandardTestContainer{
    def ITI18 = "xds-iti18://localhost:${port}/xds-iti18"

    @BeforeClass
    static void classSetUp() {
        startServer(new CXFServlet(), 'context.xml', false, 9091)
    }

    /**
     * POST directory with single folder item
     */
    @Test
    void testPostDirectorySingle() {
        // 1st test submission via openEHR API and translation of folder
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
                given()
                    .header("Ehr-Session", "3h8q0f")
                    .body(IOUtils.toString(this.getClass().getResource("directory_single.json"), "UTF-8"))
                .when()
                    .post("/ehr/12345/directory")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if submitted correctly into XDS
        def query = new FindFoldersQuery()
        // given through mocked ehr_status here in prototype state
        query.patientId = new Identifiable("_valid_root_uid_::_extension_", new AssigningAuthority("1.3"))
        query.status = [AvailabilityStatus.APPROVED]
        def queryReg = new QueryRegistry(query)
        queryReg.returnType = QueryReturnType.LEAF_CLASS
        def queryResponse = send(ITI18, queryReg, QueryResponse.class)
        assertEquals(queryResponse.toString(), Status.SUCCESS, queryResponse.status)
        //Todo: fix that this will only work when this test gets exec alone. when testMulti runs before, actually >2 folder are available. why is this not isolated?
        assertEquals(2, queryResponse.folders.size())  // 2 because test folder has sub-folder

        def testJson = new JsonSlurper().parseText(IOUtils.toString(this.getClass().getResource("directory_single.json"), "UTF-8"))
        // for each subfolder from test set: check if there is an entry in result set matching
        if (testJson.folders instanceof List) {
            testJson.folders.each { entry ->
                // by checking if there's an entry with expected coded title
                String name = testJson.name.value.toString() + "/" + entry.name.value.toString()
                def match = queryResponse.folders.find {
                    it.getTitle().value == name
                }
                assertNotEquals("Sub-folder generation mismatch", null, match)
            }

        }
    }

    /**
     * DELETE directory
     */
    @Test
    void testDeleteDirectory() {
        def store = ContextFacade.getBean(DataStore) // allows debugging watch on store

        // pre-step: submit folders to delete later
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
                given()
                    .header("Ehr-Session", "3h8q0f")
                    .body(IOUtils.toString(this.getClass().getResource("directory_single.json"), "UTF-8"))
                .when()
                    .post("/ehr/12345/directory")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 1st test deletion via openEHR API and translation of request
        def json = new JsonSlurper().parseText(response.asString())
        String test = json.uid.value.toString()

        response =
                given()
                    .header("If-Match", json.uid.value.toString())  // assign just created folder to delete
                .when()
                    .delete("/ehr/12345/directory")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if soft-deleted correctly from XDS
        // TODO: following this direct approach, querying approach doesn't work. not sure if on purpose or error. same works for documentEntries though.
        def folder = store.entries.findAll {it instanceof Folder}.find {it.availabilityStatus == AvailabilityStatus.DEPRECATED}
        assertEquals("urn:uuid:" + test, folder.entryUuid)
    }

    /**
     * PUT directory
     */
    @Test
    void testUpdateDirectory() {
        def store = ContextFacade.getBean(DataStore) // allows debugging watch on store

        // pre-step: submit folders to delete later
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
            given()
                .header("Ehr-Session", "3h8q0f")
                .body(IOUtils.toString(this.getClass().getResource("directory_single.json"), "UTF-8"))
            .when()
                .post("/ehr/12345/directory")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 1st: issue actual updating
        def respJson = new JsonSlurper().parseText(response.asString())
        def testFileJson = new JsonSlurper().parseText(IOUtils.toString(this.getClass().getResource("directory_single.json"), "UTF-8"))
        testFileJson.name.value = "Othername1"  // change name to simulate updated folder object
        def test = testFileJson.remove("folders")  // and remove sub-folders for simplicity

        def jsonString = new JsonBuilder()
        response =
            given()
                .header("If-Match", respJson.uid.value.toString())  // assign just created folder to update
                .body(JsonOutput.toJson(testFileJson))                            //
            .when()
                .put("/ehr/12345/directory")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if submitted correctly into XDS
        def query = new FindFoldersQuery()
        // given through mocked ehr_status here in prototype state
        query.patientId = new Identifiable("_valid_root_uid_::_extension_", new AssigningAuthority("1.3"))
        query.status = [AvailabilityStatus.APPROVED]
        def queryReg = new QueryRegistry(query)
        queryReg.returnType = QueryReturnType.LEAF_CLASS
        def queryResponse = send(ITI18, queryReg, QueryResponse.class)
        assertEquals(queryResponse.toString(), Status.SUCCESS, queryResponse.status)
        //Todo: fix that this will only work when this test gets exec alone. when testMulti runs before, actually >2 folder are available. why is this not isolated?
        assertEquals(2, queryResponse.folders.size())  // 2 because test folder has sub-folder

        def result = queryResponse.folders.find { it.getTitle().value == "Othername1" }
        assertNotNull(result)
    }
}
