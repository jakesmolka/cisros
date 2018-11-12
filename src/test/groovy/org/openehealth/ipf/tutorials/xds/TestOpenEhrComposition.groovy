package org.openehealth.ipf.tutorials.xds

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.apache.cxf.transport.servlet.CXFServlet
import org.junit.BeforeClass
import org.junit.Test
import org.openehealth.ipf.commons.core.config.ContextFacade
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status
import org.openehealth.ipf.platform.camel.ihe.ws.StandardTestContainer

import static com.jayway.restassured.RestAssured.given
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.Response

/**
 * Tests against openEHR REST API /composition endpoint.
 */
class TestOpenEhrComposition extends StandardTestContainer {
    def ITI18 = "xds-iti18://localhost:${port}/xds-iti18"

    @BeforeClass
    static void classSetUp() {
        startServer(new CXFServlet(), 'context.xml', false, 9091)
    }

    /**
     * POST composition, with full featured composition data
     */
    @Test
    void testPostComposition() {
        def store = ContextFacade.getBean(DataStore) // allows debugging watch on store

        // 1st test openEHR API processing and translation to XDS
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
            given()
                .body(IOUtils.toString(this.getClass().getResource("composition.json"), "UTF-8"))
            .when()
                .post("/ehr/12345/composition")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if submitted correctly into XDS
        def query = new FindDocumentsQuery()
        // given through mocked ehr_status here in prototype state
        query.patientId = new Identifiable("_valid_root_uid_::_extension_", new AssigningAuthority("1.3"))
        query.status = [AvailabilityStatus.APPROVED]
        def queryReg = new QueryRegistry(query)
        queryReg.returnType = QueryReturnType.LEAF_CLASS
        def queryResponse = send(ITI18, queryReg, QueryResponse.class)
        assertEquals(queryResponse.toString(), Status.SUCCESS, queryResponse.status)
        assertEquals(1, queryResponse.documentEntries.size())
        // entryUuid given through test file "composition.json" - and all openEHR Id are modified with "urn:uuid:[...]"
        assertEquals("urn:uuid:8849182c-82ad-4088-a07f-48ead4180515::example.domain.com::1", queryResponse.documentEntries[0].getEntryUuid())
    }

    /**
     * DELETE composition
     */
    @Test
    void testDeleteComposition() {
        def store = ContextFacade.getBean(DataStore) // allows debugging watch on store

        // pre step: posting composition to delete later
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
            given()
                .header("Ehr-Session", "3h8q0f")
                .body(IOUtils.toString(this.getClass().getResource("composition.json"), "UTF-8"))
            .when()
                .post("/ehr/12345/composition")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 1st test openEHR API processing and translation to XDS
        def json = new JsonSlurper().parse(this.getClass().getResource("composition.json"))

        response =
            given()
            .when()
                .delete("/ehr/12345/composition/" + json.uid.value)

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if soft-deleted correctly from XDS
        def query = new FindDocumentsQuery()
        // given through mocked ehr_status here in prototype state
        query.patientId = new Identifiable("_valid_root_uid_::_extension_", new AssigningAuthority("1.3"))
        query.status = [AvailabilityStatus.DEPRECATED]
        def queryReg = new QueryRegistry(query)
        queryReg.returnType = QueryReturnType.LEAF_CLASS
        def queryResponse = send(ITI18, queryReg, QueryResponse.class)
        assertEquals(queryResponse.toString(), Status.SUCCESS, queryResponse.status)
        assertEquals(1, queryResponse.documentEntries.size())
        // entryUuid given through test file "composition.json" - and all openEHR Id are modified with "urn:uuid:[...]"
        assertEquals("urn:uuid:" + json.uid.value, queryResponse.documentEntries[0].getEntryUuid())
    }

    /**
     * PUT composition
     */
    @Test
    void testUpdateComposition() {
        def store = ContextFacade.getBean(DataStore) // allows debugging watch on store

        // pre step: posting composition to update later
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        Response response =
            given()
                .header("Ehr-Session", "3h8q0f")
                .body(IOUtils.toString(this.getClass().getResource("composition.json"), "UTF-8"))
            .when()
                .post("/ehr/12345/composition")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 1st test openEHR API processing and translation to XDS
        def json = new JsonSlurper().parse(this.getClass().getResource("composition.json"))
        String oldVersion = json.uid.value
        json.uid.value = "8849182c-82ad-4088-a07f-48ead4180515::example.domain.com::2"  // change to simulate updated composition

        response =
            given()
                .body(JsonOutput.toJson(json))
            .when()
                .put("/ehr/12345/composition/" + oldVersion)

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // 2nd check if updated correctly in XDS
        def query = new FindDocumentsQuery()
        // given through mocked ehr_status here in prototype state
        query.patientId = new Identifiable("_valid_root_uid_::_extension_", new AssigningAuthority("1.3"))
        query.status = [AvailabilityStatus.APPROVED]
        def queryReg = new QueryRegistry(query)
        queryReg.returnType = QueryReturnType.LEAF_CLASS
        def queryResponse = send(ITI18, queryReg, QueryResponse.class)
        assertEquals(queryResponse.toString(), Status.SUCCESS, queryResponse.status)
        // expect only one approved, with given uid
        assertEquals(1, queryResponse.documentEntries.size())
        // entryUuid given through test file "composition.json" - and all openEHR Id are modified with "urn:uuid:[...]"
        assertEquals("urn:uuid:" + json.uid.value, queryResponse.documentEntries[0].getEntryUuid())
    }

    /**
     * Prototype feature test - mocking the openEHR CDR's GET functionality
     */
    @Test
    void testGetComposition() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8088

        def json = new JsonSlurper().parse(this.getClass().getResource("composition.json"))

        // first create composition
        Response response =
            given()
                .body(JsonOutput.toJson(json))
            .when()
                .post("/ehr/12345/composition")

        assertNotNull(response)
        assertEquals(response.toString(), 200, response.statusCode())

        // next retrieve composition
        Response postResponse =
            given()
            .when()
                .get("/ehr/12345/composition/" + json.uid.value)

        assertNotNull(postResponse)
        assertEquals(postResponse.toString(), 200, postResponse.statusCode())
        assertEquals("retrieved composition not equal written one", JsonOutput.toJson(json), postResponse.body.asString())
    }
}
