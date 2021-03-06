/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chex.archive.ejb.jdbc;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.security.auth.Subject;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmValueException;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.VRs;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PIDWithIssuer;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SecurityUtils;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.jdbc.Match.Node;

/**
 * @author <a href="mailto:gunterze@gmail.com">Gunter Zeilinger</a>
 * @version $Revision: 14783 $ $Date: 2011-01-31 18:54:43 +0800 (周一, 31 1月 2011) $
 */
public abstract class QueryCmd extends BaseDSQueryCmd {
    
    private static final int[] MATCHING_PATIENT_KEYS = new int[] {
            Tags.PatientID, 
            Tags.IssuerOfPatientID, 
            Tags.OtherPatientIDSeq,
            Tags.PatientName, 
            Tags.PatientBirthDate, 
            Tags.PatientBirthTime,
            Tags.PatientSex,
            };

    private static final int[] MATCHING_STUDY_KEYS = new int[] {
            Tags.StudyInstanceUID, 
            Tags.StudyID, 
            Tags.StudyDate,
            Tags.StudyTime, 
            Tags.AccessionNumber, 
            Tags.IssuerOfAccessionNumberSeq, 
            Tags.ReferringPhysicianName,
            Tags.StudyDescription,
            Tags.StudyStatusID,
            };

    private static final int[] MATCHING_SERIES_KEYS = new int[] {
            Tags.SeriesInstanceUID, 
            Tags.SeriesNumber,
            Tags.Modality,
            Tags.ModalitiesInStudy, 
            Tags.InstitutionName,
            Tags.InstitutionCodeSeq,
            Tags.StationName,
            Tags.SeriesDescription,
            Tags.InstitutionalDepartmentName,
            Tags.PerformingPhysicianName,
            Tags.BodyPartExamined,
            Tags.Laterality,
            Tags.PPSStartDate,
            Tags.PPSStartTime, 
            Tags.RequestAttributesSeq,
            };

    private static final int[] MATCHING_INSTANCE_KEYS = new int[] {
            Tags.SOPInstanceUID, 
            Tags.SOPClassUID, 
            Tags.InstanceNumber,
            Tags.VerificationFlag, 
            Tags.ContentDate, 
            Tags.ContentTime,
            Tags.CompletionFlag, 
            Tags.VerificationFlag,
            Tags.ConceptNameCodeSeq,
            Tags.VerifyingObserverSeq,
            Tags.ContentSeq };

    private static final int[] MATCHING_VERIFYING_OBSERVER = new int[] {
            Tags.VerificationDateTime,
            Tags.VerifyingObserverName };

    private static final int[] MATCHING_CODE_ITEM_KEYS= new int[] {
            Tags.CodeValue,
            Tags.CodingSchemeDesignator,
            Tags.CodingSchemeVersion };

    private static final int[] MATCHING_CONTENT_ITEM_KEYS= new int[] {
            Tags.RelationshipType,
            Tags.ConceptNameCodeSeq,
            Tags.TextValue,
            Tags.ConceptCodeSeq };

    private static final int[] MATCHING_REQ_ATTR_KEYS = new int[] {
            Tags.AccessionNumber,
            Tags.IssuerOfAccessionNumberSeq,
            Tags.StudyInstanceUID,
            Tags.RequestedProcedureID,
            Tags.SPSID, 
            Tags.RequestingService,
            Tags.RequestingPhysician };

    private static final int[] MATCHING_ENTITY_ID_KEYS = new int[] {
            Tags.LocalNamespaceEntityID,
            Tags.UniversalEntityID,
            Tags.UniversalEntityIDType };

    private static final int[] PAT_DEMOGRAPHICS_ATTRS = new int[] {
            Tags.PatientName,
            Tags.PatientBirthDate,
            Tags.PatientSex };

    private static final String[] AVAILABILITY = { 
            "ONLINE", "NEARLINE", "OFFLINE", "UNAVAILABLE" };

    private static final String SR_CODE = "sr_code";

    private static final String CONCEPT_CODE = "concept_code";

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;
    public static int seriesBlobAccessType = Types.BLOB;
    public static boolean lazyFetchSeriesAttrsOnImageLevelQuery = false;
    public static boolean cacheSeriesAttrsOnImageLevelQuery = true;

    private final HashMap<String,Dataset> chkPatAttrs = new HashMap<String,Dataset>();

    protected final Set<PIDWithIssuer> pidWithIssuers;

    protected final Dataset requestedIssuerOfAccessionNumber;

    protected final AdjustPatientID adjustPatientID;

    protected final boolean noMatchWithoutIssuerOfPID;

    protected final boolean fuzzyMatchingOfPN;

    protected final Subject subject;

    public static QueryCmd create(Dataset keys,
            Set<PIDWithIssuer> pidWithIssuers,  boolean filterResult,
            boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        String qrLevel = keys.getString(Tags.QueryRetrieveLevel);
        if ("IMAGE".equals(qrLevel))
            return createInstanceQuery(keys, pidWithIssuers,
                    filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("SERIES".equals(qrLevel))
            return createSeriesQuery(keys, pidWithIssuers,
                    filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("STUDY".equals(qrLevel))
            return createStudyQuery(keys, pidWithIssuers,
                    filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("PATIENT".equals(qrLevel))
            return createPatientQuery(keys, pidWithIssuers, filterResult,
                    fuzzyMatchingOfPN, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        throw new IllegalArgumentException("QueryRetrieveLevel=" + qrLevel);
    }

    public static PatientQueryCmd createPatientQuery(Dataset keys,
            Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
            boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final PatientQueryCmd cmd = new PatientQueryCmd(keys, pidWithIssuers,
                filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static StudyQueryCmd createStudyQuery(Dataset keys,
            Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
            boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final StudyQueryCmd cmd = new StudyQueryCmd(keys, pidWithIssuers,
                filterResult, fuzzyMatchingOfPN,noMatchForNoValue,
                noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static SeriesQueryCmd createSeriesQuery(Dataset keys,
            Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
            boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final SeriesQueryCmd cmd = new SeriesQueryCmd(keys, pidWithIssuers,
                filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static ImageQueryCmd createInstanceQuery(Dataset keys,
            Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
            boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final ImageQueryCmd cmd = new ImageQueryCmd(keys, pidWithIssuers,
                filterResult, fuzzyMatchingOfPN, noMatchForNoValue,
                noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    protected QueryCmd(Dataset keys, Set<PIDWithIssuer> pidWithIssuers,
            boolean filterResult, boolean fuzzyMatchingOfPN,
            boolean noMatchForNoValue, boolean noMatchWithoutIssuerOfPID,
            Subject subject) throws SQLException {
        super(keys, filterResult, noMatchForNoValue, transactionIsolationLevel);
        this.pidWithIssuers = pidWithIssuers;
        this.fuzzyMatchingOfPN = fuzzyMatchingOfPN;
        this.noMatchWithoutIssuerOfPID = noMatchWithoutIssuerOfPID;
        this.subject = subject;
        if (!keys.contains(Tags.SpecificCharacterSet)) {
            keys.putCS(Tags.SpecificCharacterSet);
        }
        matchingKeys.add(Tags.QueryRetrieveLevel);
        adjustPatientID = pidWithIssuers != null
                ? new AdjustPatientID(keys, pidWithIssuers)
                : null;
        requestedIssuerOfAccessionNumber =
                keys.getItem(Tags.IssuerOfAccessionNumberSeq);
    }

    protected void addAdditionalReturnKeys() {
        keys.putAE(Tags.RetrieveAET);
        keys.putSH(Tags.StorageMediaFileSetID);
        keys.putUI(Tags.StorageMediaFileSetUID);
        keys.putCS(Tags.InstanceAvailability);
    }

    protected void init() {
        sqlBuilder.setSelect(getSelectAttributes());
        sqlBuilder.setFrom(getTables());
        sqlBuilder.setLeftJoin(getLeftJoin());
        sqlBuilder.setRelations(getRelations());
    }

    protected abstract String[] getSelectAttributes();

    protected abstract String[] getTables();

    protected String[] getLeftJoin() {
        return null;
    }

    protected String[] getRelations() {
        return null;
    }

    public boolean isMatchNotSupported() {
        return sqlBuilder.isMatchNotSupported();
    }

    protected void addPatientMatch() {
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
        sqlBuilder.addLiteralMatch(null, "Patient.merge_fk", false, "IS NULL");
        if (pidWithIssuers != null) {
            Node n = sqlBuilder.addNodeMatch("OR", false);
            for (PIDWithIssuer pwi : pidWithIssuers) {
                Node n1 = new Match.Node("AND", false);
                n1.addMatch(new Match.SingleValue(null, "Patient.patientId", type2,
                        pwi.pid));
                n1.addMatch(new Match.SingleValue(null, "Patient.issuerOfPatientId",
                        type2, pwi.issuer));
                n.addMatch(n1);
            }
        } else {
            String[] pid = filter.getStrings(keys, Tags.PatientID);
            String issuer = filter.getString(keys, Tags.IssuerOfPatientID);
            Match matchPID = sqlBuilder.addWildCardMatch(null, "Patient.patientId", type2, pid);
            if (matchPID != null && issuer != null) {
                sqlBuilder.addSingleValueMatch(null, "Patient.issuerOfPatientId",
                        type2, issuer);
            } else if (noMatchWithoutIssuerOfPID) {
                sqlBuilder.addNULLValueMatch(null,"Patient.issuerOfPatientId",
                        true);
            }
        }
        if (fuzzyMatchingOfPN)
            sqlBuilder.addPNFuzzyMatch(
                    new String[] { "Patient.patientFamilyNameSoundex",
                            "Patient.patientGivenNameSoundex" },
                            keys.getString(Tags.PatientName));
        else
            sqlBuilder.addPNMatch(
                    new String[] { "Patient.patientName",
                            "Patient.patientIdeographicName",
                            "Patient.patientPhoneticName" }, type2,
                            filter.isICase(Tags.PatientName),
                            keys.getString(Tags.PatientName));
        sqlBuilder
                .addRangeMatch(null, "Patient.patientBirthDate", type2,
                        keys.getString(Tags.PatientBirthDate));
        sqlBuilder.addWildCardMatch(null, "Patient.patientSex", type2,
                filter.getStrings(keys, Tags.PatientSex));
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Patient." + filter.getField(fieldTags[i]), type2,
                    filter.getStrings(keys, fieldTags[i]));
            
        }
        matchingKeys.add(MATCHING_PATIENT_KEYS);
        matchingKeys.add(fieldTags);
    }

    protected void addStudyMatch() {
        AttributeFilter filter = AttributeFilter.getStudyAttributeFilter();
        sqlBuilder.addListOfUidMatch(null, "Study.studyIuid", SqlBuilder.TYPE1,
                keys.getStrings(Tags.StudyInstanceUID));
        sqlBuilder.addWildCardMatch(null, "Study.studyId", type2,
                filter.getStrings(keys, Tags.StudyID));
        sqlBuilder.addRangeMatch(null, "Study.studyDateTime", type2, keys
                .getDateTimeRange(Tags.StudyDate, Tags.StudyTime));
        if (sqlBuilder.addWildCardMatch(null, "Study.accessionNumber", type2,
                filter.getStrings(keys, Tags.AccessionNumber)) != null) {
            Dataset issuer = keys.getItem(Tags.IssuerOfAccessionNumberSeq);
            if (isMatchIssuer(issuer)) {
                SqlBuilder subQuery = new SqlBuilder();
                subQuery.setSelect(new String[] { "Issuer.pk" });
                subQuery.setFrom(new String[] { "Issuer" });
                subQuery.addFieldValueMatch(null, "Issuer.pk",
                        SqlBuilder.TYPE1, null, "Study.accno_issuer_fk");
                subQuery.addSingleValueMatch(null,
                        "Issuer.localNamespaceEntityID", type2,
                        issuer.getString(Tags.LocalNamespaceEntityID));
                subQuery.addSingleValueMatch(null, "Issuer.universalEntityID",
                        type2, issuer.getString(Tags.UniversalEntityID));
                subQuery.addSingleValueMatch(null,
                        "Issuer.universalEntityIDType", type2,
                        issuer.getString(Tags.UniversalEntityIDType));
                Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
                node0.addMatch(new Match.Subquery(subQuery, null, null));
            }
        }
        sqlBuilder.addPNMatch(new String[] { "Study.referringPhysicianName",
                "Study.referringPhysicianIdeographicName",
                "Study.referringPhysicianPhoneticName" }, type2,
                filter.isICase(Tags.ReferringPhysicianName),
                keys.getString(Tags.ReferringPhysicianName));
        sqlBuilder.addWildCardMatch(null, "Study.studyDescription", type2,
                filter.getStrings(keys, Tags.StudyDescription));
        sqlBuilder.addListOfStringMatch(null, "Study.studyStatusId", type2,
                filter.getStrings(keys, Tags.StudyStatusID));
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Study." + filter.getField(fieldTags[i]), type2,
                    filter.getStrings(keys, fieldTags[i]));
            
        }
        matchingKeys.add(MATCHING_STUDY_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(Tags.IssuerOfAccessionNumberSeq,
                new IntList().add(MATCHING_ENTITY_ID_KEYS));
    }


    protected void addStudyPermissionMatch(boolean patientLevel) {
        if (subject != null) {
            sqlBuilder.addQueryPermissionNestedMatch(patientLevel, false,
                    SecurityUtils.rolesOf(subject));
        }
    }

    
    protected void addNestedSeriesMatch() {
        sqlBuilder.addModalitiesInStudyNestedMatch(null, keys
                .getStrings(Tags.ModalitiesInStudy));
        sqlBuilder.addCallingAETsNestedMatch(false, getCallingAETs(keys));
        matchingKeys.add(Tags.ModalitiesInStudy);
        matchingKeys.add(PrivateTags.CallingAET);
    }

    private String[] getCallingAETs(Dataset ds) {
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.CallingAET);
        ds.setPrivateCreatorID(null);
        try {
            if (bb != null)
                return StringUtils.split(
                        new String(bb.array(), "UTF-8").trim(), '\\');
        } catch (UnsupportedEncodingException ignore) {}
        return new String[]{};
    }

    protected void addSeriesMatch() {
        AttributeFilter filter = AttributeFilter.getSeriesAttributeFilter();
        sqlBuilder.addListOfUidMatch(null, "Series.seriesIuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SeriesInstanceUID));
        sqlBuilder.addWildCardMatch(null, "Series.seriesNumber", type2,
                filter.getStrings(keys, Tags.SeriesNumber));
        sqlBuilder.addWildCardMatch(null, "Series.modality", type2,
                filter.getStrings(keys, Tags.Modality));
        sqlBuilder.addWildCardMatch(null, "Series.seriesNumber", type2,
                filter.getStrings(keys, Tags.SeriesNumber));
        sqlBuilder.addWildCardMatch(null, "Series.bodyPartExamined", type2,
                filter.getStrings(keys, Tags.BodyPartExamined));
        sqlBuilder.addWildCardMatch(null, "Series.laterality", type2,
                filter.getStrings(keys, Tags.Laterality));
        sqlBuilder.addWildCardMatch(null, "Series.institutionName", type2,
                filter.getStrings(keys, Tags.InstitutionName));
        sqlBuilder.addWildCardMatch(null, "Series.stationName", type2,
                filter.getStrings(keys, Tags.StationName));
        sqlBuilder.addWildCardMatch(null, "Series.seriesDescription", type2,
                filter.getStrings(keys, Tags.SeriesDescription));
        sqlBuilder.addWildCardMatch(null, "Series.institutionalDepartmentName",
                type2, filter.getStrings(keys, Tags.InstitutionalDepartmentName));
        sqlBuilder.addPNMatch(new String[] { "Series.performingPhysicianName",
                "Series.performingPhysicianIdeographicName",
                "Series.performingPhysicianPhoneticName" }, type2,
                filter.isICase(Tags.PerformingPhysicianName),
                keys.getString(Tags.PerformingPhysicianName));
        sqlBuilder.addRangeMatch(null, "Series.ppsStartDateTime", type2, keys
                .getDateTimeRange(Tags.PPSStartDate, Tags.PPSStartTime));
        sqlBuilder.addListOfStringMatch(null, "Series.sourceAET", type2, getCallingAETs(keys));

        if (this.isMatchRequestAttributes()) {
            SqlBuilder subQuery = new SqlBuilder();
            subQuery.setSelect(new String[] { "SeriesRequest.pk" });
            subQuery.setFrom(new String[] { "SeriesRequest" });
            subQuery.addFieldValueMatch(null, "Series.pk", SqlBuilder.TYPE1, null,
                    "SeriesRequest.series_fk");
            Dataset rqAttrs = keys.getItem(Tags.RequestAttributesSeq);
            subQuery.addListOfUidMatch(null, "SeriesRequest.studyIuid", type2,
                    rqAttrs.getStrings(Tags.StudyInstanceUID));
            subQuery.addWildCardMatch(null,
                    "SeriesRequest.requestedProcedureId", SqlBuilder.TYPE1,
                            filter.getStrings(rqAttrs, Tags.RequestedProcedureID));
            subQuery.addWildCardMatch(null, "SeriesRequest.spsId", SqlBuilder.TYPE1,
                    filter.getStrings(rqAttrs,Tags.SPSID));
            subQuery.addWildCardMatch(null, "SeriesRequest.requestingService",
                    type2, filter.getStrings(rqAttrs, Tags.RequestingService));
            subQuery.addPNMatch(new String[] {
                    "SeriesRequest.requestingPhysician",
                    "SeriesRequest.requestingPhysicianIdeographicName",
                    "SeriesRequest.requestingPhysicianPhoneticName" }, type2,
                    filter.isICase(Tags.RequestingPhysician),
                    rqAttrs.getString(Tags.RequestingPhysician));
            if (subQuery.addWildCardMatch(null,
                    "SeriesRequest.accessionNumber", type2,
                    filter.getStrings(rqAttrs, Tags.AccessionNumber)) != null) {
                Dataset issuer = rqAttrs.getItem(Tags.IssuerOfAccessionNumberSeq);
                if (isMatchIssuer(issuer)) {
                    SqlBuilder subQuery2 = new SqlBuilder();
                    subQuery2.setSelect(new String[] { "Issuer.pk" });
                    subQuery2.setFrom(new String[] { "Issuer" });
                    subQuery2.addFieldValueMatch(null, "Issuer.pk",
                            SqlBuilder.TYPE1, null,
                            "SeriesRequest.accno_issuer_fk");
                    subQuery2.addSingleValueMatch(null,
                            "Issuer.localNamespaceEntityID", type2, issuer
                                    .getString(Tags.LocalNamespaceEntityID));
                    subQuery2.addSingleValueMatch(null,
                            "Issuer.universalEntityID", type2, issuer
                                    .getString(Tags.UniversalEntityID));
                    subQuery2.addSingleValueMatch(null,
                            "Issuer.universalEntityIDType", type2, issuer
                                    .getString(Tags.UniversalEntityIDType));
                    subQuery.addNodeMatch("OR", false).addMatch(
                            new Match.Subquery(subQuery2, null, null));
                }
            }
            Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
            node0.addMatch(new Match.Subquery(subQuery, null, null));
        }

        Dataset code = keys.getItem(Tags.InstitutionCodeSeq);
        if (isMatchCode(code)) {
            SqlBuilder subQuery = new SqlBuilder();
            subQuery.setSelect(new String[] { "Code.pk" });
            subQuery.setFrom(new String[] { "Code" });
            subQuery.addFieldValueMatch(null, "Code.pk", SqlBuilder.TYPE1, null,
                    "Series.inst_code_fk");
            subQuery.addSingleValueMatch(null, "Code.codeValue", type2,
                    code.getString(Tags.CodeValue));
            subQuery.addSingleValueMatch(null,
                    "Code.codingSchemeDesignator", type2,
                    code.getString(Tags.CodingSchemeDesignator));
            subQuery.addSingleValueMatch(null,
                    "Code.codingSchemeVersion", type2,
                    code.getString(Tags.CodingSchemeVersion));
            Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
            node0.addMatch(new Match.Subquery(subQuery, null, null));
        }

        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Series." + filter.getField(fieldTags[i]), type2,
                    filter.getStrings(keys, fieldTags[i]));
            
        }

        matchingKeys.add(MATCHING_SERIES_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(new Integer(Tags.RequestAttributesSeq),
                new IntList().add(MATCHING_REQ_ATTR_KEYS));
        seqMatchingKeys.put(new Integer(Tags.InstitutionCodeSeq),
                new IntList().add(MATCHING_CODE_ITEM_KEYS));
    }

    protected void addInstanceMatch() {
        AttributeFilter filter = AttributeFilter.getInstanceAttributeFilter(null);
        sqlBuilder.addListOfUidMatch(null, "Instance.sopIuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SOPInstanceUID));
        sqlBuilder.addListOfUidMatch(null, "Instance.sopCuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SOPClassUID));
        sqlBuilder.addWildCardMatch(null, "Instance.instanceNumber", type2,
                filter.getStrings(keys, Tags.InstanceNumber));
        sqlBuilder.addRangeMatch(null, "Instance.contentDateTime", type2, keys
                .getDateTimeRange(Tags.ContentDate, Tags.ContentTime));
        sqlBuilder.addSingleValueMatch(null, "Instance.srCompletionFlag",
                type2, filter.getString(keys, Tags.CompletionFlag));
        sqlBuilder.addSingleValueMatch(null, "Instance.srVerificationFlag",
                type2, filter.getString(keys, Tags.VerificationFlag));
        Dataset code = keys.getItem(Tags.ConceptNameCodeSeq);
        if (code != null) {
            sqlBuilder.addSingleValueMatch(SR_CODE, "Code.codeValue", type2,
                    code.getString(Tags.CodeValue));
            sqlBuilder.addSingleValueMatch(SR_CODE,
                    "Code.codingSchemeDesignator", type2, code
                            .getString(Tags.CodingSchemeDesignator));
            sqlBuilder.addSingleValueMatch(SR_CODE,
                    "Code.codingSchemeVersion", type2, code
                            .getString(Tags.CodingSchemeVersion));
       }
        if (this.isMatchVerifyingObserver()) {
            Dataset voAttrs = keys.getItem(Tags.VerifyingObserverSeq);

            SqlBuilder subQuery = new SqlBuilder();
            subQuery.setSelect(new String[] { "VerifyingObserver.pk" });
            subQuery.setFrom(new String[] { "VerifyingObserver" });
            subQuery.addFieldValueMatch(null, "Instance.pk", SqlBuilder.TYPE1, null,
                    "VerifyingObserver.instance_fk");
            subQuery.addRangeMatch(null,
                    "VerifyingObserver.verificationDateTime", SqlBuilder.TYPE1,
                    voAttrs.getDateRange(Tags.VerificationDateTime));
            subQuery.addPNMatch(new String[] {
                    "VerifyingObserver.verifyingObserverName",
                    "VerifyingObserver.verifyingObserverIdeographicName",
                    "VerifyingObserver.verifyingObserverPhoneticName" },
                    SqlBuilder.TYPE1,
                    filter.isICase(Tags.VerifyingObserverName),
                    voAttrs.getString(Tags.VerifyingObserverName));

            Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
            node0.addMatch(new Match.Subquery(subQuery, null, null));
        }
        DcmElement contentSeq = keys.get(Tags.ContentSeq);
        if (contentSeq != null) {
            Match.Node node0 = null;
            for (int i = 0, n = contentSeq.countItems(); i < n; i++) {
                Dataset item = contentSeq.getItem(i);
                String relType = item.getString(Tags.RelationshipType);
                String textValue = item.getString(Tags.TextValue);
                Dataset conceptName = item.getItem(Tags.ConceptNameCodeSeq);
                if (!isMatchCode(conceptName))
                    conceptName = null;
                Dataset conceptCode = item.getItem(Tags.ConceptCodeSeq);
                if (!isMatchCode(conceptCode))
                    conceptCode = null;
                if (conceptName != null || conceptCode != null
                        || textValue != null || relType != null) {
                    String[] entities;
                    String[] aliases = null;
                    if (conceptName != null) {
                        if (conceptCode != null) {
                            entities = new String[] { "ContentItem", "Code", "Code" };
                            aliases = new String[] { null, null, CONCEPT_CODE };
                        } else {
                            entities = new String[] { "ContentItem", "Code" };
                        }
                    } else {
                        if (conceptCode != null) {
                            entities = new String[] { "ContentItem", "Code" };
                            aliases = new String[] { null, CONCEPT_CODE };
                        } else {
                            entities = new String[] { "ContentItem" };
                        }
                    }
                    SqlBuilder subQuery = new SqlBuilder();
                    subQuery.setSelect(new String[] { "ContentItem.pk" });
                    subQuery.setFrom(entities);
                    subQuery.setAliases(aliases);
                    subQuery.addFieldValueMatch(null, "Instance.pk", 
                            SqlBuilder.TYPE1, null, "ContentItem.instance_fk");
                    subQuery.addSingleValueMatch(null, "ContentItem.relationshipType",
                            SqlBuilder.TYPE1, relType);
                    subQuery.addWildCardMatch(null, "ContentItem.textValue",
                            SqlBuilder.TYPE1, textValue);
                    subQuery.addCodeMatch(null, conceptName);
                    subQuery.addCodeMatch(CONCEPT_CODE, conceptCode);
                    if (node0 == null)
                        node0 = sqlBuilder.addNodeMatch("AND", false);
                    node0.addMatch(new Match.Subquery(subQuery, null, null));
                }
            }
         }
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Instance." + filter.getField(fieldTags[i]), type2,
                    filter.getStrings(keys, fieldTags[i]));
        }
        matchingKeys.add(MATCHING_INSTANCE_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(new Integer(Tags.ConceptNameCodeSeq),
                new IntList().add(MATCHING_CODE_ITEM_KEYS));
        seqMatchingKeys.put(new Integer(Tags.VerifyingObserverSeq),
                new IntList().add(MATCHING_VERIFYING_OBSERVER));
        seqMatchingKeys.put(new Integer(Tags.ContentSeq),
                new IntList().add(MATCHING_CONTENT_ITEM_KEYS));
    }

    public Dataset getDataset() throws SQLException {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        fillDataset(ds);
        if (pidWithIssuers != null)
            checkForDiffPatientDemographics(ds);
        if (adjustPatientID != null)
            adjustPatientID.adjust(ds);
        adjustAccessionNumber(ds);
        adjustDataset(ds, keys);
        return filterResult ? ds.subSet(keys) : ds;
    }

    private void adjustAccessionNumber(Dataset ds) {
        if (requestedIssuerOfAccessionNumber == null)
            return;

        Dataset issuer = ds.getItem(Tags.IssuerOfAccessionNumberSeq); 
        if (issuer == null 
                || issuer.match(requestedIssuerOfAccessionNumber, false, true))
            return;

        ds.putSQ(Tags.IssuerOfAccessionNumberSeq)
                .addItem(requestedIssuerOfAccessionNumber);
        if (ds.contains(Tags.AccessionNumber))
            ds.putSH(Tags.AccessionNumber);
    }

    private void checkForDiffPatientDemographics(Dataset ds) {
        String patId = getPatIdString(ds);
        if (!chkPatAttrs.containsKey(patId)) {
            for (Entry<String, Dataset> entry : chkPatAttrs.entrySet()) {
                String patId2 = entry.getKey();
                Dataset ds2 = entry.getValue();
                for (int tag : PAT_DEMOGRAPHICS_ATTRS) {
                    DcmElement elem = ds.get(tag);
                    DcmElement elem2 = ds2.get(tag);
                    if (log.isDebugEnabled())
                        log.debug("compare:" + elem + " with " + elem2);
                    if (elem != null && elem2 != null && !checkAttr(elem, elem2)) {
                        log.warn("Different patient attribute found! " + patId
                                + elem + " <-> " + patId2 + elem2);
                    }
                }
            }
            chkPatAttrs.put(patId, ds);
        }
    }

    private boolean checkAttr(DcmElement elem, DcmElement elem1) {
        if (elem.isEmpty() && elem1.isEmpty())
            return true;
        if (elem.vr() == VRs.PN) {
            return getFnGn(elem).equals(getFnGn(elem1));
        }
        return elem.equals(elem1);
    }

    private String getFnGn(DcmElement el) {
        try {
            String pn = el.getString(null);
            if (pn == null) {
                return "";
            }
            int pos = pn.indexOf('=');
            if (pos != -1)
                pn = pn.substring(0, pos);
            pos = pn.indexOf('^');
            if (pos != -1) {
                pos = pn.indexOf('^', pos);
                return pos != -1 ? pn.substring(0, pos) : pn;
            } else {
                return pn;
            }
        } catch (DcmValueException x) {
            log.error("Cant get family and given name value of " + el, x);
            return "";
        }
    }

    private String getPatIdString(Dataset ds) {
        return ds.getString(Tags.PatientID) + "^"
                + ds.getString(Tags.IssuerOfPatientID);
    }

    protected abstract void fillDataset(Dataset ds) throws SQLException;

    protected void fillDataset(Dataset ds, int column) throws SQLException {
        DatasetUtils.fromByteArray(rs.getBytes(column), ds);
    }

    public static class PatientQueryCmd extends QueryCmd {

        protected PatientQueryCmd(Dataset keys,
                Set<PIDWithIssuer> pidWithIssuers,  boolean filterResult,
                boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
        throws SQLException {
            super(keys, pidWithIssuers, filterResult, fuzzyMatchingOfPN,
                    noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
            defineColumnTypes(new int[] { blobAccessType });
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyPermissionMatch(true);
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            ds.putCS(Tags.QueryRetrieveLevel, "PATIENT");
        }

        protected String[] getSelectAttributes() {
            return new String[] { "Patient.encodedAttributes" };
        }

        protected String[] getTables() {
            return new String[] { "Patient" };
        }

    }

    public static class StudyQueryCmd extends QueryCmd {

        protected StudyQueryCmd(Dataset keys,
                Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
                boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
             super(keys, pidWithIssuers, filterResult, fuzzyMatchingOfPN,
                    noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
             defineColumnTypes(new int[] {
                    blobAccessType,     // Patient.encodedAttributes
                    blobAccessType,     // Study.encodedAttributes
                    Types.VARCHAR,      // Study.modalitiesInStudy
                    Types.VARCHAR,      // Study.sopClassesInStudy
                    Types.VARCHAR,      // Study.studyStatusId
                    Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                    Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                    Types.VARCHAR,      // Study.filesetId
                    Types.VARCHAR,      // Study.filesetIuid
                    Types.VARCHAR,      // Study.retrieveAETs
                    Types.VARCHAR,      // Study.externalRetrieveAET
                    Types.INTEGER       // Study.availability
                    });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addNestedSeriesMatch();
        }

        protected String[] getSelectAttributes() {
            return new String[] { 
                    "Patient.encodedAttributes",                // (1)
                    "Study.encodedAttributes",                  // (2)
                    "Study.modalitiesInStudy",                  // (3)
                    "Study.sopClassesInStudy",                  // (4)
                    "Study.studyStatusId",                      // (5)
                    "Study.numberOfStudyRelatedSeries",         // (6)
                    "Study.numberOfStudyRelatedInstances",      // (7)
                    "Study.filesetId",                          // (8)
                    "Study.filesetIuid",                        // (9)
                    "Study.retrieveAETs",                       // (10)
                    "Study.externalRetrieveAET",                // (11)
                    "Study.availability",                       // (12)
                    };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study" };
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk" };
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            fillDataset(ds, 2);
            ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(rs.getString(3),
                    '\\'));
            ds.putUI(Tags.SOPClassesInStudy, StringUtils.split(rs.getString(4),
                    '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(5));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(6));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(7));
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(8));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(9));
            DatasetUtils.putRetrieveAET(ds, rs.getString(10), rs.getString(11));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(12)]);
            ds.putCS(Tags.QueryRetrieveLevel, "STUDY");
        }

    }

    public static class SeriesQueryCmd extends QueryCmd {

        protected SeriesQueryCmd(Dataset keys,
                Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
                boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
             super(keys, pidWithIssuers, filterResult, fuzzyMatchingOfPN,
                    noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
             defineColumnTypes(new int[] {
                    blobAccessType,     // Patient.encodedAttributes
                    blobAccessType,     // Study.encodedAttributes
                    blobAccessType,     // Series.encodedAttributes
                    Types.VARCHAR,      // Study.modalitiesInStudy
                    Types.VARCHAR,      // Study.sopClassesInStudy
                    Types.VARCHAR,      // Study.studyStatusId
                    Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                    Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                    Types.INTEGER,      // Series.numberOfSeriesRelatedInstances
                    Types.VARCHAR,      // Series.filesetId
                    Types.VARCHAR,      // Series.filesetIuid
                    Types.VARCHAR,      // Series.retrieveAETs
                    Types.VARCHAR,      // Series.externalRetrieveAET
                    Types.VARCHAR,      // Series.sourceAET
                    Types.INTEGER,      // Series.availability
                    });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addNestedSeriesMatch();
            addSeriesMatch();
        }

        protected String[] getSelectAttributes() {
            return new String[] {
                    "Patient.encodedAttributes",                // (1)
                    "Study.encodedAttributes",                  // (2)
                    "Series.encodedAttributes",                 // (3)
                    "Study.modalitiesInStudy",                  // (4)
                    "Study.sopClassesInStudy",                  // (5)
                    "Study.studyStatusId",                      // (6)
                    "Study.numberOfStudyRelatedSeries",         // (7)
                    "Study.numberOfStudyRelatedInstances",      // (8)
                    "Series.numberOfSeriesRelatedInstances",    // (9)
                    "Series.filesetId",                         // (10)
                    "Series.filesetIuid",                       // (11)
                    "Series.retrieveAETs",                      // (12)
                    "Series.externalRetrieveAET",               // (13)
                    "Series.sourceAET",                         // (14)
                    "Series.availability",                      // (15)
                    };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study", "Series" };
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk",
                            "Study.pk", "Series.study_fk" };
        }

        protected String[] getLeftJoin() {
            return null;
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            fillDataset(ds, 2);
            fillDataset(ds, 3);
            ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(rs.getString(4),
                    '\\'));
            ds.putUI(Tags.SOPClassesInStudy, StringUtils.split(rs.getString(5),
                    '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(6));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(7));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(8));
            ds.putIS(Tags.NumberOfSeriesRelatedInstances, rs.getInt(9));
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(10));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(11));
            DatasetUtils.putRetrieveAET(ds, rs.getString(12), rs.getString(13));
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putSH(PrivateTags.CallingAET, rs.getString(14));
            ds.setPrivateCreatorID(null);
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(15)]);
            ds.putCS(Tags.QueryRetrieveLevel, "SERIES");
        }
    }

    public static class ImageQueryCmd extends QueryCmd {

        HashMap<String,Dataset> seriesAttrsCache =
                new HashMap<String,Dataset>();

        protected ImageQueryCmd(Dataset keys,
                Set<PIDWithIssuer> pidWithIssuers, boolean filterResult,
                boolean fuzzyMatchingOfPN, boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
             super(keys, pidWithIssuers, filterResult, fuzzyMatchingOfPN,
                    noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
             defineColumnTypes(lazyFetchSeriesAttrsOnImageLevelQuery
                    ? new int[] {
                            blobAccessType,     // Instance.encodedAttributes
                            Types.VARCHAR,      // Instance.retrieveAETs
                            Types.VARCHAR,      // Instance.externalRetrieveAET
                            Types.INTEGER,      // Instance.availability
                            Types.TIMESTAMP,    // Instance.updatedTime
                            Types.VARCHAR,      // Series.seriesIuid
                            Types.VARCHAR,      // Media.filesetId
                            Types.VARCHAR,      // Media.filesetIuid
                            }
                    : new int[] {
                            blobAccessType,     // Instance.encodedAttributes
                            Types.VARCHAR,      // Series.seriesIuid
                            blobAccessType,     // Patient.encodedAttributes
                            blobAccessType,     // Study.encodedAttributes
                            blobAccessType,     // Series.encodedAttributes
                            Types.VARCHAR,      // Study.modalitiesInStudy
                            Types.VARCHAR,      // Study.sopClassesInStudy
                            Types.VARCHAR,      // Study.studyStatusId
                            Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                            Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                            Types.INTEGER,      // Series.numberOfSeriesRelatedInstances
                            Types.VARCHAR,      // Series.sourceAET
                            Types.VARCHAR,      // Instance.retrieveAETs
                            Types.VARCHAR,      // Instance.externalRetrieveAET
                            Types.INTEGER,      // Instance.availability
                            Types.TIMESTAMP,    // Instance.updatedTime
                            Types.VARCHAR,      // Media.filesetId
                            Types.VARCHAR,      // Media.filesetIuid
                            });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addNestedSeriesMatch();
            addSeriesMatch();
            addInstanceMatch();
        }

        protected String[] getSelectAttributes() {
            return lazyFetchSeriesAttrsOnImageLevelQuery
                    ? new String[] {
                            "Instance.encodedAttributes",               // (1)
                            "Instance.retrieveAETs",                    // (2)
                            "Instance.externalRetrieveAET",             // (3)
                            "Instance.availability",                    // (4)
                            "Instance.updatedTime",                     // (5)
                            "Series.seriesIuid",                        // (6)
                            "Media.filesetId",                          // (7)
                            "Media.filesetIuid",                        // (8)
                            }
                    : new String[] {
                            "Instance.encodedAttributes",               // (1)
                            "Series.seriesIuid",                        // (2)
                            "Patient.encodedAttributes",                // (3)
                            "Study.encodedAttributes",                  // (4)
                            "Series.encodedAttributes",                 // (5)
                            "Study.modalitiesInStudy",                  // (6)
                            "Study.sopClassesInStudy",                  // (7)
                            "Study.studyStatusId",                      // (8)
                            "Study.numberOfStudyRelatedSeries",         // (9)
                            "Study.numberOfStudyRelatedInstances",      // (10)
                            "Series.numberOfSeriesRelatedInstances",    // (11)
                            "Series.sourceAET",                         // (12)
                            "Instance.retrieveAETs",                    // (13)
                            "Instance.externalRetrieveAET",             // (14)
                            "Instance.availability",                    // (15)
                            "Instance.updatedTime",                     // (16)
                            "Media.filesetId",                          // (17)
                            "Media.filesetIuid",                        // (18)
                            };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study", "Series", "Instance" };
        }

        protected String[] getLeftJoin() {
            return isMatchCode(keys.getItem(Tags.ConceptNameCodeSeq)) ? new String[] {
                        "Code", SR_CODE, "Instance.srcode_fk", "Code.pk",
                        "Media", null, "Instance.media_fk", "Media.pk" }
                    : new String[] {
                        "Media", null, "Instance.media_fk", "Media.pk" };
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk",
                            "Study.pk", "Series.study_fk", "Series.pk",
                            "Instance.series_fk" };
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            if (lazyFetchSeriesAttrsOnImageLevelQuery) {
                fillDatasetWithLazyFetchSeriesAttrs(ds);
            } else {
                fillDatasetWithEagerFetchSeriesAttrs(ds);
            }
        }
        
        private void fillDatasetWithLazyFetchSeriesAttrs(Dataset ds)
                throws SQLException {
            fillDataset(ds, 1);
            DatasetUtils.putRetrieveAET(ds, rs.getString(2), rs.getString(3));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(4)]);
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putDT(PrivateTags.InstanceUpdated, rs.getTimestamp(5));
            ds.setPrivateCreatorID(null);
            String seriesIuid = rs.getString(6);
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(7));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(8));
            Dataset seriesAttrs = (Dataset) seriesAttrsCache.get(seriesIuid);
            if (seriesAttrs == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Lazy fetch Series attributes for Series "
                            + seriesIuid);
                }
                QuerySeriesAttrsForQueryCmd seriesQuery =
                        new QuerySeriesAttrsForQueryCmd(
                                QueryCmd.transactionIsolationLevel,
                                QueryCmd.seriesBlobAccessType,
                                seriesIuid);
                try {
                    seriesQuery.execute();
                    seriesQuery.next();
                    seriesAttrs = seriesQuery.getDataset();
                } finally {
                    seriesQuery.close();
                }
                seriesAttrsCache.put(seriesIuid, seriesAttrs);
            }
            ds.putAll(seriesAttrs);
            ds.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        }
        
        private void fillDatasetWithEagerFetchSeriesAttrs(Dataset ds)
                throws SQLException {
            fillDataset(ds, 1);
            if (cacheSeriesAttrsOnImageLevelQuery) {
                String seriesIuid = rs.getString(2);
                Dataset seriesAttrs = (Dataset) seriesAttrsCache.get(seriesIuid);
                if (seriesAttrs == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache Series attributes for Series "
                                + seriesIuid);
                    }
                    seriesAttrs = DcmObjectFactory.getInstance().newDataset();
                    fillDataset(seriesAttrs, 3);
                    fillDataset(seriesAttrs, 4);
                    fillDataset(seriesAttrs, 5);
                    seriesAttrsCache.put(seriesIuid, seriesAttrs);
                } else if (log.isDebugEnabled()) {
                    log.debug("Use cached Series attributes for Series "
                            + seriesIuid);
                }
                ds.putAll(seriesAttrs);
            } else {
                fillDataset(ds, 3);
                fillDataset(ds, 4);
                fillDataset(ds, 5);
            }
            ds.putCS(Tags.ModalitiesInStudy,
                    StringUtils.split(rs.getString(6), '\\'));
            ds.putUI(Tags.SOPClassesInStudy,
                    StringUtils.split(rs.getString(7), '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(8));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(9));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(10));
            ds.putIS(Tags.NumberOfSeriesRelatedInstances, rs.getInt(11));
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putSH(PrivateTags.CallingAET, rs.getString(12));
            DatasetUtils.putRetrieveAET(ds, rs.getString(13), rs.getString(14));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(15)]);
            ds.putDT(PrivateTags.InstanceUpdated, rs.getTimestamp(16));
            ds.setPrivateCreatorID(null);
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(17));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(18));
            ds.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        }
    }
    
    private static boolean isMatchCode(Dataset code) {
        return code != null
                && (code.containsValue(Tags.CodeValue)
                        || code.containsValue(Tags.CodingSchemeDesignator));
    }

    private static boolean isMatchIssuer(Dataset issuer) {
        return issuer != null 
                && (issuer.containsValue(Tags.LocalNamespaceEntityID)
                        ||  issuer.containsValue(Tags.UniversalEntityID));
    }

    protected boolean isMatchRequestAttributes() {
        Dataset rqAttrs = keys.getItem(Tags.RequestAttributesSeq);
        return rqAttrs != null
                && (rqAttrs.containsValue(Tags.RequestedProcedureID)
                        || rqAttrs.containsValue(Tags.SPSID)
                        || rqAttrs.containsValue(Tags.RequestingService)
                        || rqAttrs.containsValue(Tags.RequestingPhysician)
                        || rqAttrs.containsValue(Tags.StudyInstanceUID)
                        || rqAttrs.containsValue(Tags.AccessionNumber));
    }

    protected boolean isMatchVerifyingObserver() {
        Dataset item = keys.getItem(Tags.VerifyingObserverSeq);
        return item != null
                && (item.containsValue(Tags.VerificationDateTime)
                        || item.containsValue(Tags.VerifyingObserverName));
    }

}