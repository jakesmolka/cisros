/*
 * Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openehealth.ipf.commons.ihe.xds.core.audit.event;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.types.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.core.atna.AuditDataset;
import org.openehealth.ipf.commons.ihe.core.atna.event.PHIExportBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.audit.XdsAuditDataset;
import org.openehealth.ipf.commons.ihe.xds.core.audit.XdsNonconstructiveDocumentSetRequestAuditDataset;
import org.openehealth.ipf.commons.ihe.xds.core.audit.XdsSubmitAuditDataset;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Christian Ohr
 */
public class XdsPHIExportBuilder extends PHIExportBuilder<XdsPHIExportBuilder> {

    public XdsPHIExportBuilder(AuditContext auditContext,
                               XdsAuditDataset auditDataset,
                               EventType eventType,
                               PurposeOfUse... purposesOfUse) {
        this(auditContext, auditDataset, EventActionCode.Read, eventType, purposesOfUse);
    }

    public XdsPHIExportBuilder(AuditContext auditContext,
                               XdsAuditDataset auditDataset,
                               EventActionCode eventActionCode,
                               EventType eventType,
                               PurposeOfUse... purposesOfUse) {
        super(auditContext, auditDataset, eventActionCode, eventType, purposesOfUse);
    }

    public XdsPHIExportBuilder(AuditContext auditContext,
                               AuditDataset auditDataset,
                               EventOutcomeIndicator eventOutcomeIndicator,
                               String eventOutcomeDescription,
                               EventActionCode eventActionCode,
                               EventType eventType,
                               PurposeOfUse... purposesOfUse) {
        super(auditContext, auditDataset, eventOutcomeIndicator, eventOutcomeDescription, eventActionCode, eventType, purposesOfUse);
    }

    public XdsPHIExportBuilder setSubmissionSet(XdsSubmitAuditDataset auditDataset) {
        return addExportedEntity(auditDataset.getSubmissionSetUuid(),
                ParticipantObjectIdTypeCode.XdsMetadata,
                ParticipantObjectTypeCodeRole.Job,
                Collections.emptyList());
    }

    public XdsPHIExportBuilder setSubmissionSetWithHomeCommunityId(XdsSubmitAuditDataset auditDataset) {
        return addExportedEntity(auditDataset.getSubmissionSetUuid(),
                ParticipantObjectIdTypeCode.XdsMetadata,
                ParticipantObjectTypeCodeRole.Job,
                makeDocumentDetail(null, auditDataset.getHomeCommunityId(), null, null));
    }

    public XdsPHIExportBuilder addDocumentIds(XdsNonconstructiveDocumentSetRequestAuditDataset auditDataset,
                                              XdsNonconstructiveDocumentSetRequestAuditDataset.Status status) {
        String[] documentIds = auditDataset.getDocumentIds(status);
        String[] homeCommunityIds = auditDataset.getHomeCommunityIds(status);
        String[] repositoryIds = auditDataset.getRepositoryIds(status);
        String[] seriesInstanceIds = auditDataset.getSeriesInstanceIds(status);
        String[] studyInstanceIds = auditDataset.getStudyInstanceIds(status);
        IntStream.range(0, documentIds.length).forEach(i ->
                addExportedEntity(
                        documentIds[i],
                        ParticipantObjectIdTypeCode.ReportNumber,
                        ParticipantObjectTypeCodeRole.Report,
                        makeDocumentDetail(repositoryIds[i], homeCommunityIds[i], seriesInstanceIds[i], studyInstanceIds[i])));
        return self();
    }


}
