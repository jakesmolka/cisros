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
package org.openehealth.ipf.tutorials.xds

import ca.uhn.hl7v2.model.v281.datatype.DTM
import org.joda.time.DateTime
import org.openehr.rm.datatypes.quantity.datetime.DvDateTime

import java.security.MessageDigest
import org.apache.commons.io.IOUtils

/**
 * Utility functionality for document content.
 * @author Jens Riemschneider
 */
abstract class ContentUtils {
    private ContentUtils() {
        throw new UnsupportedOperationException('Cannot be instantiated')
    }
    
    /**
     * Calculates the size of the given content stream.
     * @param dataHandler
     *          the data handler to access the content.
     * @return the size in bytes.
     */
    static def size(dataHandler) {
        def content = getContent(dataHandler)
        def size = content.length
        size
    }

    /**
     * Calculates the SHA-1 of the given content stream.
     * @param dataHandler
     *          the data handler to access the content.
     * @return the SHA-1.
     */
    static def sha1(dataHandler) {
        def content = getContent(dataHandler)
        calcSha1(content)
    }

    private static def calcSha1(content) {
        def digest = MessageDigest.getInstance('SHA-1')
        def builder = new StringBuilder()
        digest.digest(content).each {
            def hexString = Integer.toHexString((int)it & 0xff)
            builder.append(hexString.length() == 2 ? hexString : '0' + hexString)
        }
        builder.toString()
    }

    /**
     * Retrieves the byte array from a datahandler.
     * @param dataHandler
     *          the data handler to access the content.
     * @return the content as a byte[].
     */
    static def getContent(dataHandler) {
        def inputStream = dataHandler.inputStream
        try {
            IOUtils.toByteArray(inputStream)
        }
        finally {
            inputStream.close()
        }
    }

    /**
     * Small util function to HTTP GET on remote openEHR REST
     * @param URL to invoke HTTP GET on
     * @return Response body as string
     */
    static String httpGet(URL url) {
        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod("GET")
        BufferedReader ins = new BufferedReader(
                new InputStreamReader(con.getInputStream()))
        String inputLine
        StringBuffer content = new StringBuffer()
        while ((inputLine = ins.readLine()) != null) {
            content.append(inputLine)
        }
        ins.close()
        con.disconnect()
        content.toString()
    }

    /**
     * Small util function to HTTP POST on remote openEHR REST
     * @param URL to invoke HTTP POST on
     * @param payload for request body
     * @return Response body as string
     */
    static String httpPost(URL url, String body) {
        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod("POST")
        con.setDoOutput(true)
        con.setRequestProperty("Prefer", "return=representation")
        byte[] outputInBytes = body.getBytes("UTF-8")
        OutputStream os = con.getOutputStream()
        os.write( outputInBytes )
        os.close()

        BufferedReader ins = new BufferedReader(
                new InputStreamReader(con.getInputStream()))
        String inputLine
        StringBuffer content = new StringBuffer()
        while ((inputLine = ins.readLine()) != null) {
            content.append(inputLine)
        }
        ins.close()
        con.disconnect()
        content.toString()
    }

    /**
     * Converts openEHR DvDateTime string into HL7 DTM string.
     * @param openEhrTime as string
     * @return DTM time as string
     */
    static String openEhrTimeToHl7Time(String openEhrTime) {
        DvDateTime dvDateTime = new DvDateTime(openEhrTime)
        DateTime dateTime = dvDateTime.getDateTime()
        Calendar cal = dateTime.toCalendar()
        DTM dtm = new DTM()
        dtm.setValue(cal)
        dtm.toString()
    }
}
