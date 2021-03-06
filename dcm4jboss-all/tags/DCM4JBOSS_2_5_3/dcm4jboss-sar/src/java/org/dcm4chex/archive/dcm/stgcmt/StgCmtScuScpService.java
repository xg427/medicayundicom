/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/
package org.dcm4chex.archive.dcm.stgcmt;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.JMException;
import javax.management.ObjectName;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.PDU;
import org.dcm4che.net.RoleSelection;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScpService;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;
import org.dcm4chex.archive.ejb.jdbc.AECmd;
import org.dcm4chex.archive.ejb.jdbc.AEData;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.exceptions.UnkownAETException;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.JMSDelegate;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 1948 $ $Date: 2005-09-05 17:15:49 +0800 (周一, 05 9月 2005) $
 * @since Jan 5, 2005
 */
public class StgCmtScuScpService extends AbstractScpService implements
        MessageListener {

	private static final int MSG_ID = 1;

	private static final int ERR_STGCMT_RJ = -2;

	private static final int ERR_ASSOC_RJ = -1;

	private static final int PCID_STGCMT = 1;
	
    private ObjectName fileSystemMgtName;
    
    private String queueName = "StgCmtScuScp";

    private TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);

    private int acTimeout = 5000;

    private int dimseTimeout = 0;

    private int soCloseDelay = 500;

    private RetryIntervalls scuRetryIntervalls = new RetryIntervalls();

    private RetryIntervalls scpRetryIntervalls = new RetryIntervalls();

    private StgCmtScuScp stgCmtScuScp = new StgCmtScuScp(this);

    private long receiveResultInSameAssocTimeout;

	private int concurrency = 1;

	public final int getConcurrency() {
		return concurrency;
	}

	public final void setConcurrency(int concurrency) throws Exception {
		if (concurrency <= 0)
			throw new IllegalArgumentException("Concurrency: " + concurrency);
		if (this.concurrency != concurrency) {
			final boolean restart = getState() == STARTED;
			if (restart)
				stop();
			this.concurrency = concurrency;
			if (restart)
				start();
		}
	}

    public final String getScuRetryIntervalls() {
        return scuRetryIntervalls.toString();
    }

    public final void setScuRetryIntervalls(String s) {
        this.scuRetryIntervalls = new RetryIntervalls(s);
    }

    public final String getScpRetryIntervalls() {
        return scpRetryIntervalls.toString();
    }

    public final void setScpRetryIntervalls(String s) {
        this.scpRetryIntervalls = new RetryIntervalls(s);
    }

    public final ObjectName getTLSConfigName() {
        return tlsConfig.getTLSConfigName();
    }

    public final void setTLSConfigName(ObjectName tlsConfigName) {
        tlsConfig.setTLSConfigName(tlsConfigName);
    }

    public final String getQueueName() {
        return queueName;
    }
    
    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public String getEjbProviderURL() {
        return EJBHomeFactory.getEjbProviderURL();
    }

    public final ObjectName getFileSystemMgtName() {
        return fileSystemMgtName;
    }

    public final void setFileSystemMgtName(ObjectName fileSystemMgtName) {
        this.fileSystemMgtName = fileSystemMgtName;
    }

    public void setEjbProviderURL(String ejbProviderURL) {
        EJBHomeFactory.setEjbProviderURL(ejbProviderURL);
    }

    public final int getAcTimeout() {
        return acTimeout;
    }

    public final void setAcTimeout(int acTimeout) {
        this.acTimeout = acTimeout;
    }

    public final int getDimseTimeout() {
        return dimseTimeout;
    }

    public final void setDimseTimeout(int dimseTimeout) {
        this.dimseTimeout = dimseTimeout;
    }

    public final int getSoCloseDelay() {
        return soCloseDelay;
    }

    public final void setSoCloseDelay(int soCloseDelay) {
        this.soCloseDelay = soCloseDelay;
    }

    public final long getReceiveResultInSameAssocTimeout() {
        return receiveResultInSameAssocTimeout;
    }
    
    public final void setReceiveResultInSameAssocTimeout(long timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeout: " + timeout);
        this.receiveResultInSameAssocTimeout = timeout;
    }
    
    protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.StorageCommitmentPushModel, stgCmtScuScp);
    }

    protected void unbindDcmServices(DcmServiceRegistry services) {
        services.unbind(UIDs.StorageCommitmentPushModel);
    }

    protected void updatePresContexts(AcceptorPolicy policy, boolean enable) {
        if (enable) {
            policy.putPresContext(UIDs.StorageCommitmentPushModel,
                    getTransferSyntaxUIDs());
            policy.putRoleSelection(UIDs.StorageCommitmentPushModel, true, true);
        } else {
            policy.putPresContext(UIDs.StorageCommitmentPushModel, null);
            policy.removeRoleSelection(UIDs.StorageCommitmentPushModel);            
        }
    }

    Socket createSocket(AEData aeData) throws IOException {
        return tlsConfig.createSocket(aeData);
    }

    boolean isLocalFileSystem(String dirpath) {
        try {
            Boolean b = (Boolean) server.invoke(fileSystemMgtName,
                    "isLocalFileSystem", new Object[] { dirpath },
                    new String[] { String.class.getName() });
            return b.booleanValue();
        } catch (JMException e) {
            throw new RuntimeException("Failed to invoke isLocalFileSystem", e);
        }
    }

    public void queueStgCmtOrder(String calling, String called,
            Dataset actionInfo, boolean scpRole) throws JMSException {
        StgCmtOrder order = new StgCmtOrder(calling, called, actionInfo, scpRole);
        JMSDelegate.queue(queueName, order, 0, 0);        
    }
    
    protected void startService() throws Exception {
        super.startService();
        JMSDelegate.startListening(queueName, this, concurrency);
    }

    protected void stopService() throws Exception {
        JMSDelegate.stopListening(queueName);
        super.stopService();
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            StgCmtOrder order = (StgCmtOrder) om.getObject();
            log.info("Start processing " + order);
			try {
				process(order);
                log.info("Finished processing " + order);
			} catch (Exception e) {
	            final int failureCount = order.getFailureCount() + 1;
	            order.setFailureCount(failureCount);
	            final RetryIntervalls retryIntervalls = order.isScpRole() 
	                    ? scpRetryIntervalls : scuRetryIntervalls;
	            final long delay = retryIntervalls.getIntervall(failureCount);
	            if (delay == -1L) {
	                log.error("Give up to process " + order, e);
	            } else {
	                log.warn("Failed to process " + order + ". Scheduling retry.", e);
	                JMSDelegate.queue(queueName, order, 0,
	                        System.currentTimeMillis() + delay);
	            }
			}
        } catch (JMSException e) {
            log.error("jms error during processing message: " + message, e);
        } catch (Throwable e) {
            log.error("unexpected error during processing message: " + message,
                    e);
        }

    }

    private Dataset commit(StgCmtOrder order) {
		int failureReason = Status.ProcessingFailure;
		Storage storage = null;
		try {
			StorageHome home = (StorageHome) EJBHomeFactory.getFactory()
					.lookup(StorageHome.class, StorageHome.JNDI_NAME);
			storage = home.create();
		} catch (Exception e) {
			log.error("Failed to access Storage EJB", e);
		}
		Dataset actionInfo = order.getActionInfo();
		DcmElement refSOPSeq = actionInfo.get(Tags.RefSOPSeq);
		Map fileInfos = null;
		if (storage != null) {
			try {
				FileInfo[][] aa = RetrieveCmd.create(refSOPSeq).getFileInfos();
				fileInfos = new HashMap();
				for (int i = 0; i < aa.length; i++) {
					fileInfos.put(aa[i][0].sopIUID, aa[i]);
				}
			} catch (SQLException e) {
				log.error("Failed to query DB", e);
			}
		}
		Dataset eventInfo = DcmObjectFactory.getInstance().newDataset();
		eventInfo.putUI(Tags.TransactionUID, actionInfo
				.getString(Tags.TransactionUID));
		DcmElement successSOPSeq = eventInfo.putSQ(Tags.RefSOPSeq);
		DcmElement failedSOPSeq = eventInfo.putSQ(Tags.FailedSOPSeq);
		for (int i = 0, n = refSOPSeq.vm(); i < n; ++i) {
			Dataset refSOP = refSOPSeq.getItem(i);
			if (storage != null
					&& fileInfos != null
					&& (failureReason = commit(storage, refSOP, fileInfos)) == Status.Success) {
				successSOPSeq.addItem(refSOP);
			} else {
				refSOP.putUS(Tags.FailureReason, failureReason);
				failedSOPSeq.addItem(refSOP);
			}
		}
		if (failedSOPSeq.isEmpty()) {
			eventInfo.remove(Tags.FailedSOPSeq);
		}
		return eventInfo;
	}

	private void process(StgCmtOrder order)
			throws InterruptedException, DcmServiceException, IOException,
			UnkownAETException, SQLException {
		final String aet = order.getCalledAET();
		AEData aeData = new AECmd(aet).getAEData();
		if (aeData == null) {
			throw new UnkownAETException("Unkown Retrieve AET: " + aet);
		}
		Dataset ds = order.isScpRole() ? commit(order) : order.getActionInfo();
		AssociationFactory af = AssociationFactory.getInstance();
		Association a = af.newRequestor(tlsConfig.createSocket(aeData));
		a.setAcTimeout(acTimeout);
		a.setDimseTimeout(dimseTimeout);
		a.setSoCloseDelay(soCloseDelay);
		AAssociateRQ rq = af.newAAssociateRQ();
		rq.setCalledAET(aet);
		rq.setCallingAET(order.getCallingAET());
		rq.addPresContext(af.newPresContext(PCID_STGCMT,
				UIDs.StorageCommitmentPushModel, NATIVE_LE_TS));
		if (order.isScpRole()) {
			rq.addRoleSelection(af.newRoleSelection(
					UIDs.StorageCommitmentPushModel, false, true));
		}
		PDU ac = a.connect(rq);
		if (!(ac instanceof AAssociateAC)) {
			throw new DcmServiceException(ERR_ASSOC_RJ,
					"Association not accepted by " + aet + ": " + ac);
		}
		ActiveAssociation aa = af.newActiveAssociation(a, null);
		aa.start();
		try {
			if (a.getAcceptedTransferSyntaxUID(PCID_STGCMT) == null)
				throw new DcmServiceException(ERR_STGCMT_RJ,
						"StgCmt not supported by remote AE: " + aet);
			Command cmd = DcmObjectFactory.getInstance().newCommand();
			if (order.isScpRole()) {
				RoleSelection rs = ((AAssociateAC) ac)
						.getRoleSelection(UIDs.StorageCommitmentPushModel);
				if (rs == null || !rs.scp()) {
					log.warn("SCU Role of Storage Commitment Service rejected by "
									+ aet
									+ " - try to send N_EVENT_REPORT anyway");
				}
				cmd.initNEventReportRQ(1, UIDs.StorageCommitmentPushModel,
						UIDs.StorageCommitmentPushModelSOPInstance, ds
								.contains(Tags.FailedSOPSeq) ? 2 : 1);
				invokeDimse(aa, cmd, ds, "StgCmt Result:\n");
			} else {
				cmd.initNActionRQ(MSG_ID, UIDs.StorageCommitmentPushModel,
						UIDs.StorageCommitmentPushModelSOPInstance, 1);
				ds.putUI(Tags.TransactionUID, 
						UIDGenerator.getInstance().createUID());
				invokeDimse(aa, cmd, ds, "StgCmt Request:\n");
				Thread.sleep(receiveResultInSameAssocTimeout);
			}
		} finally {
			try {
				aa.release(true);
			} catch (Exception e) {
				log.warn(
						"Failed to release association " + aa.getAssociation(),
						e);
			}
		}
	}

	private void invokeDimse(ActiveAssociation aa, Command cmd, Dataset ds,
			String prompt) throws InterruptedException, IOException,
			DcmServiceException {
		log.debug(prompt);
		log.debug(ds);
		final AssociationFactory af = AssociationFactory.getInstance();
		Dimse rsp = aa.invoke(af.newDimse(PCID_STGCMT, cmd, ds)).get();
		final Command cmdRsp = rsp.getCommand();
		int status = cmdRsp.getStatus();
		if (status != 0)
			throw new DcmServiceException(status, cmdRsp
					.getString(Tags.ErrorComment));
	}

	private int commit(Storage storage, Dataset refSOP, Map fileInfos) {
        final String iuid = refSOP.getString(Tags.RefSOPInstanceUID);
        final String cuid = refSOP.getString(Tags.RefSOPClassUID);
        FileInfo[] fileInfo = (FileInfo[]) fileInfos.get(iuid);
        if (fileInfo == null) {
            log.warn("Failed Storage Commitment of Instance[uid=" + iuid
                    + "]: no such object");
            return Status.NoSuchObjectInstance;
        }
        if (!fileInfo[0].sopCUID.equals(cuid)) {
            log.warn("Failed Storage Commitment of Instance[uid=" + iuid
                    + "]: SOP Class in request[" + cuid
                    + "] does not match SOP Class in stored object["
                    + fileInfo[0].sopCUID + "]");
            return Status.ClassInstanceConflict;
        }
        try {
            LinkedHashSet retrieveAETs = new LinkedHashSet();
            for (int i = 0; i < fileInfo.length; i++) {
                retrieveAETs.add(fileInfo[i].fileRetrieveAET);
                checkFile(fileInfo[i]);
            }
            storage.commit(iuid);
            retrieveAETs.add(fileInfo[0].extRetrieveAET);
            retrieveAETs.remove(null);
            if (!retrieveAETs.isEmpty())
                refSOP.putAE(Tags.RetrieveAET, (String[]) retrieveAETs
                        .toArray(new String[retrieveAETs.size()]));
            return Status.Success;
        } catch (Exception e) {
            log.error("Failed Storage Commitment of Instance[uid="
                    + fileInfo[0].sopIUID + "]:", e);
            return Status.ProcessingFailure;
        }
    }

    private void checkFile(FileInfo info) throws IOException {
        if (info.basedir == null || !isLocalFileSystem(info.basedir))
            return;
        File file = FileUtils.toFile(info.basedir, info.fileID);
        log.info("M-READ file:" + file);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                file));
        DigestInputStream dis = new DigestInputStream(in, md);
        try {
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            if (parser.getReadTag() == Tags.PixelData) {
                if (parser.getReadLength() == -1) {
                    while (parser.parseHeader() == Tags.Item) {
                        readOut(parser.getInputStream(), parser.getReadLength());
                    }
                }
                readOut(parser.getInputStream(), parser.getReadLength());
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
            }
        } finally {
            try {
                dis.close();
            } catch (IOException ignore) {
            }
        }
        byte[] md5 = md.digest();
        if (!Arrays.equals(md5, info.getFileMd5())) {
            throw new IOException("MD5 mismatch");
        }
    }

    private void readOut(InputStream in, int len) throws IOException {
        int toRead = len;
        while (toRead-- > 0) {
            if (in.read() < 0) {
                throw new EOFException();
            }
        }
    }

    void commited(Dataset stgcmtResult) throws DcmServiceException {
        Storage storage = null;
        try {
            StorageHome home = (StorageHome) EJBHomeFactory.getFactory()
                    .lookup(StorageHome.class, StorageHome.JNDI_NAME);
            storage = home.create();
            storage.commited(stgcmtResult);
        } catch (Exception e) {
            log.error("Failed update External AETs in DB records", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        } finally {
            if (storage != null)
                try {
                    storage.remove();
                } catch (Exception ignore) {}
        }
    }
}
