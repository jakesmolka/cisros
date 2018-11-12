package org.openehealth.ipf.tutorials.xds

import org.apache.commons.io.IOUtils
import org.apache.cxf.transport.servlet.CXFServlet
import org.junit.BeforeClass
import org.junit.Test
import org.openehealth.ipf.commons.ihe.xds.core.SampleData
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry
import org.openehealth.ipf.commons.ihe.xds.core.requests.DocumentReference
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status
import org.openehealth.ipf.platform.camel.ihe.ws.StandardTestContainer
import static org.junit.Assert.assertEquals
import javax.activation.DataHandler
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType

import static org.junit.Assert.assertNotNull

/**
 * Simple tests against the data store.
 */
class TestDataStore {

    // TODO: test works but this functions isn't used...?!
    // DataStore.store(Document) saves each document by simply adding to list
    // Retrieving only with .get(...)
    @Test
    void testStoreDoc() {
        def provide = SampleData.createProvideAndRegisterDocumentSet()
        def docEntry = provide.documents[0].documentEntry
        def patientId = docEntry.patientId
        patientId.id = UUID.randomUUID().toString()
        docEntry.uniqueId = '4.3.2.1'
        docEntry.hash = ContentUtils.sha1(provide.documents[0].getContent(DataHandler))
        docEntry.size = ContentUtils.size(provide.documents[0].getContent(DataHandler))

        def dataStore = new DataStore()
        dataStore.store(provide.documents[0])
    }

    // TODO: why is this used for custom DSL instead of above? (i.e. each route with .store() uses this)
    // DataStore maintains 5 lists for different identifiers and .store(entry) adds entry's ID to each matching list
    // means can store DocumentEntry, Folder, SubmissionSet, Association (and more?) on their own.
    // Retrieving only with .search(...)
    @Test
    void testStoreDocEntry() {
        def dataStore = new DataStore()



        String str = "test"
        dataStore.store(str)

        // TODO add real test since importance was revealed

    }

    @Test
    void testGet() {
        // first store a document
        def provide = SampleData.createProvideAndRegisterDocumentSet()
        def docEntry = provide.documents[0].documentEntry
        def patientId = docEntry.patientId
        patientId.id = UUID.randomUUID().toString()
        docEntry.uniqueId = '4.3.2.1'
        docEntry.hash = ContentUtils.sha1(provide.documents[0].getContent(DataHandler))
        docEntry.size = ContentUtils.size(provide.documents[0].getContent(DataHandler))

        def dataStore = new DataStore()
        dataStore.store(provide.documents[0])
        // to get it
        def dataHandler = dataStore.get(docEntry.uniqueId)

        assertNotNull(dataHandler)
        assertEquals("stored data not valid", read(provide.documents[0].getDataHandler()), read(dataHandler))
    }

    @Test
    void testSearch() {
        // first store a document
        def provide = SampleData.createProvideAndRegisterDocumentSet()
        def docEntry = provide.documents[0].documentEntry
        def patientId = docEntry.patientId
        patientId.id = UUID.randomUUID().toString()
        docEntry.uniqueId = '4.3.2.1'
        docEntry.hash = ContentUtils.sha1(provide.documents[0].getContent(DataHandler))
        docEntry.size = ContentUtils.size(provide.documents[0].getContent(DataHandler))

        def dataStore = new DataStore()
        dataStore.store(provide.documents[0].documentEntry)
        // to search it
        def indexEvals = [provide.documents[0].documentEntry.uniqueId]
        //indexEvals.put("patientId", new Identifiable("id3", new AssigningAuthority("1.3")))
        def filters = []
        def param = ""
        // TODO: fix parameters
        dataStore.search(indexEvals, filters, param)

    }

    def read(dataHandler) {
        def inputStream = dataHandler.inputStream
        try {
            return IOUtils.toString(inputStream)
        }
        finally {
            inputStream.close()
        }
    }
}