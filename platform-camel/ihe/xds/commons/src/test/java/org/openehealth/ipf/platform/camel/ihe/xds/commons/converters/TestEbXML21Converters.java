/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.platform.camel.ihe.xds.commons.converters;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.SampleData;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.ebxml.ebxml21.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.requests.QueryRegistry;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.requests.RegisterDocumentSet;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.responses.QueryResponse;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.responses.Response;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.stub.ebrs21.query.AdhocQueryRequest;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.stub.ebrs21.rs.RegistryResponse;
import org.openehealth.ipf.platform.camel.ihe.xds.commons.stub.ebrs21.rs.SubmitObjectsRequest;

/**
 * Tests for {@link EbXML21Converters}.
 * @author Jens Riemschneider
 */
public class TestEbXML21Converters {
    @Test
    public void testConvertProvideAndRegisterDocumentSet() {
        ProvideAndRegisterDocumentSet org = SampleData.createProvideAndRegisterDocumentSet();
        ProvideAndRegisterDocumentSetRequestType converted = EbXML21Converters.convert(org);
        ProvideAndRegisterDocumentSet copy = EbXML21Converters.convert(converted);
        assertEquals(org, copy);
    }

    @Test
    public void testConvertRegisterDocumentSet() {
        RegisterDocumentSet org = SampleData.createRegisterDocumentSet();
        SubmitObjectsRequest converted = EbXML21Converters.convert(org);
        RegisterDocumentSet copy = EbXML21Converters.convert(converted);
        assertEquals(org, copy);
    }

    @Test
    public void testConvertResponse() {
        Response org = SampleData.createResponse();
        RegistryResponse converted = EbXML21Converters.convert(org);
        Response copy = EbXML21Converters.convert(converted);
        assertEquals(org, copy);
    }

    @Test
    public void testConvertQueryRegistry() {
        QueryRegistry org = SampleData.createSqlQuery();
        AdhocQueryRequest converted = EbXML21Converters.convert(org);
        QueryRegistry copy = EbXML21Converters.convert(converted);
        assertEquals(org, copy);
    }

    @Test
    public void testConvertQueryResponse() {
        QueryResponse org = SampleData.createQueryResponse();
        RegistryResponse converted = EbXML21Converters.convert(org);
        QueryResponse copy = EbXML21Converters.convertToQueryResponse(converted);
        assertEquals(org, copy);
    }
}