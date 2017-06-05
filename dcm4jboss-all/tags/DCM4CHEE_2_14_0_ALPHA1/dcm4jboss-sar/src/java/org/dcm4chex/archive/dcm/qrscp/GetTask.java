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
 * Agfa HealthCare.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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
package org.dcm4chex.archive.dcm.qrscp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.exceptions.NoPresContextException;
import org.jboss.logging.Logger;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since May 6, 2008
 */
class GetTask implements Runnable {

    private final QueryRetrieveScpService service;
    private final Logger log;
    private ActiveAssociation assoc;
    private final int pcid;
    private final Command rqCmd;
    private final FileInfo[][] fileInfos;
    private final ArrayList<FileInfo> transferred = new ArrayList<FileInfo>();
    private final ArrayList<String> failedIUIDs = new ArrayList<String>();

    private int warnings = 0;

    private int completed = 0;

    private int remaining = 0;

    private boolean canceled = false;

    public GetTask(QueryRetrieveScpService service, ActiveAssociation assoc,
            Dimse rq, FileInfo[][] fileInfos) {
        this.service = service;
        this.log = service.getLog();
        this.assoc = assoc;
        this.pcid = rq.pcid();
        this.rqCmd = rq.getCommand();
        this.fileInfos = fileInfos;
        this.remaining = fileInfos.length;
        if (remaining > 0) {
            assoc.addCancelListener(rqCmd.getMessageID(), new DimseListener() {
                public void dimseReceived(Association assoc, Dimse dimse) {
                    GetTask.this.canceled  = true;
                }});
        }
    }

    public void run() {
        if (fileInfos.length > 0) {
            Set<StudyInstanceUIDAndDirPath> studyInfos =
                new HashSet<StudyInstanceUIDAndDirPath>();
            Association a = assoc.getAssociation();
            TimerTask sendPendingRsp = new TimerTask() {
                public void run() {
                    if (remaining > 0) {
                        sendGetRsp(Status.Pending, null);
                    }
                }};            
            service.scheduleSendPendingRsp(sendPendingRsp);
            try {
                for (int i = 0; i < fileInfos.length; i++) {
                    final FileInfo[] fileInfo = fileInfos[i];
                    final FileInfo fileInfo0 = fileInfo[0];
                    final String iuid = fileInfo0.sopIUID;
                    DimseListener storeScpListener = new DimseListener() {

                        public void dimseReceived(Association assoc, Dimse dimse) {
                            switch (dimse.getCommand().getStatus()) {
                            case Status.Success:
                                ++completed;
                                transferred.add(fileInfo0);
                                break;
                            case Status.CoercionOfDataElements:
                            case Status.DataSetDoesNotMatchSOPClassWarning:
                            case Status.ElementsDiscarded:
                                ++warnings;
                                transferred.add(fileInfo0);
                                break;
                            default:
                                failedIUIDs.add(iuid);
                                break;
                            }
                            --remaining;
                        }
                    };
                    
                    try {
                        Dimse rq = service.makeCStoreRQ(assoc, fileInfo0,
                                rqCmd.getInt(Tags.Priority, Command.MEDIUM),
                                null, 0, null);
                        assoc.invoke(rq, storeScpListener);
                    } catch (NoPresContextException e) {
                        if (!service.isIgnorableSOPClass(fileInfo0.sopCUID, 
                                a.getCallingAET())) {
                            failedIUIDs.add(fileInfo0.sopIUID);
                            log.warn(e.getMessage());
                        } else {
                            log.info(e.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("Exception during retrieve of " + iuid, e);
                    }
                    // track access on ONLINE FS
                    if (fileInfo0.availability == Availability.ONLINE)
                        studyInfos.add(new StudyInstanceUIDAndDirPath(fileInfo0));
                }
            } finally {
                sendPendingRsp.cancel();
            }
            if (!transferred.isEmpty()) {
                service.logInstancesSent(a, a, transferred);
            }
            service.updateStudyAccessTime(studyInfos);
        }
        sendGetRsp(status(), service.makeRetrieveRspIdentifier(failedIUIDs));
    }

    private int status() {
        return canceled ? Status.Cancel 
                : failedIUIDs.isEmpty() ? Status.Success
                        : completed == 0 ? Status.UnableToPerformSuboperations
                                : Status.SubOpsOneOrMoreFailures;
    }

    private void sendGetRsp(int status, Dataset ds) {
        if (assoc == null)
            return;
        Command rspCmd = DcmObjectFactory.getInstance().newCommand();
        rspCmd.initCGetRSP(rqCmd.getMessageID(),
                rqCmd.getAffectedSOPClassUID(), status);
        if (remaining > 0) {
            rspCmd.putUS(Tags.NumberOfRemainingSubOperations, remaining);
        } else {
            rspCmd.remove(Tags.NumberOfRemainingSubOperations);
        }
        rspCmd.putUS(Tags.NumberOfCompletedSubOperations, completed);
        rspCmd.putUS(Tags.NumberOfWarningSubOperations, warnings);
        rspCmd.putUS(Tags.NumberOfFailedSubOperations, failedIUIDs.size());
        try {
            assoc.getAssociation().write(
                    AssociationFactory.getInstance().newDimse(pcid, rspCmd, ds));
        } catch (Exception e) {
            log.info("Failed to send C-GET RSP to "
                    + assoc.getAssociation().getCallingAET(), e);
            assoc  = null;
        }
    }

}