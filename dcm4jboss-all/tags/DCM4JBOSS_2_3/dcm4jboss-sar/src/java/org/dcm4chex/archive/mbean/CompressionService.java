/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/
package org.dcm4chex.archive.mbean;

import java.io.File;
import java.io.FileFilter;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.ejb.FinderException;
import javax.management.JMException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.dict.UIDs;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.codec.CodecCmd;
import org.dcm4chex.archive.codec.CompressCmd;
import org.dcm4chex.archive.codec.DecompressCmd;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgtHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 1694 $ $Date: 2005-04-14 23:16:19 +0800 (周四, 14 4月 2005) $
 * @since 12.09.2004
 *
 */
public class CompressionService extends TimerSupport {

    private static final String _DCM = ".dcm";
    
    private long taskInterval = 0L;

    private int disabledStartHour;

    private int disabledEndHour;

    private int limitNumberOfFilesPerTask;

    private boolean verifyCompression;

    private Integer listenerID;

    private ObjectName fileSystemMgtName;

    private File tmpDir = new File("tmp");

    private long keepTempFileIfVerificationFails = 0L;    

    private List compressionRuleList = new ArrayList();

    private static final String[] CODEC_NAMES = new String[] { "JPLL", "JLSL",
            "J2KR" };

    private static final String[] COMPRESS_TRANSFER_SYNTAX = new String[] {
            UIDs.JPEGLossless, UIDs.JPEGLSLossless, UIDs.JPEG2000Lossless };

    private final NotificationListener delayedCompressionListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            checkForTempFilesToDelete();
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (isDisabled(hour)) {
                if (log.isDebugEnabled())
                    log.debug("trigger ignored in time between "
                            + disabledStartHour + " and " + disabledEndHour
                            + " !");
            } else {
                try {
                    checkForFilesToCompress();
                } catch (Exception e) {
                    log.error("Delayed compression failed!", e);
                }
            }
        }
    };

    public final ObjectName getFileSystemMgtName() {
        return fileSystemMgtName;
    }

    public final void setFileSystemMgtName(ObjectName fileSystemMgtName) {
        this.fileSystemMgtName = fileSystemMgtName;
    }
    
    public final String getKeepTempFileIfVerificationFails() {
        return RetryIntervalls.formatInterval(keepTempFileIfVerificationFails);
    }
    
    public final void setKeepTempFileIfVerificationFails(String interval) {
        this.keepTempFileIfVerificationFails = 
                RetryIntervalls.parseInterval(interval);
    }
    
    public final String getTaskInterval() {
        String s = RetryIntervalls.formatIntervalZeroAsNever(taskInterval);
        return (disabledEndHour == -1) ? s : s + "!" + disabledStartHour + "-"
                + disabledEndHour;
    }

    public void setTaskInterval(String interval) {
        long oldInterval = taskInterval;
        int pos = interval.indexOf('!');
        if (pos == -1) {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval);
            disabledEndHour = -1;
        } else {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval
                    .substring(0, pos));
            int pos1 = interval.indexOf('-', pos);
            disabledStartHour = Integer.parseInt(interval.substring(pos + 1,
                    pos1));
            disabledEndHour = Integer.parseInt(interval.substring(pos1 + 1));
        }
        if (getState() == STARTED && oldInterval != taskInterval) {
            stopScheduler("CheckFilesToCompress", listenerID, delayedCompressionListener);
            listenerID = startScheduler("CheckFilesToCompress", 
            		taskInterval, delayedCompressionListener);
        }
    }

    public final void setCompressionRules(String rules) {
        this.compressionRuleList.clear();
        if (rules == null || rules.trim().length() < 1)
            return;
        StringTokenizer st = new StringTokenizer(rules, ",;\n\r\t ");
        while (st.hasMoreTokens()) {
            compressionRuleList.add(new CompressionRule(st.nextToken()));
        }
    }

    public final String getCompressionRules() {
        StringBuffer sb = new StringBuffer();
        Iterator iter = this.compressionRuleList.iterator();
        while (iter.hasNext()) {
            sb.append(((CompressionRule) iter.next())).append("\r\n");
        }
        return sb.toString();
    }

    public int getLimitNumberOfFilesPerTask() {
        return limitNumberOfFilesPerTask;
    }

    public void setLimitNumberOfFilesPerTask(int limit) {
        this.limitNumberOfFilesPerTask = limit;
    }

    public boolean isVerifyCompression() {
        return verifyCompression;
    }

    public void setVerifyCompression(boolean checkCompression) {
        this.verifyCompression = checkCompression;
    }

    public final int getMaxConcurrentCodec() {
        return CodecCmd.getMaxConcurrentCodec();
    }

    public final void setMaxConcurrentCodec(int maxConcurrentCodec) {
        CodecCmd.setMaxConcurrentCodec(maxConcurrentCodec);
    }

    public final void setTempDir( String dirPath ) {
        tmpDir = new File(dirPath);
    }
    
    public final String getTempDir() {
        return tmpDir.toString();
    }
        
    public void checkForTempFilesToDelete() {
        if (keepTempFileIfVerificationFails <= 0)
            return;
        File absTmpDir = FileUtils.resolve(tmpDir);
        if (!absTmpDir.isDirectory())
            return;
        final long before = System.currentTimeMillis() - keepTempFileIfVerificationFails;
        File[] files = absTmpDir.listFiles(new FileFilter(){
            public boolean accept(File f) {
                return f.getName().endsWith(_DCM) && f.lastModified() < before;
            }});
        for (int i = 0; i < files.length; i++) {
            if (log.isDebugEnabled())
                log.debug("M-DELETE " + files[i]);
            if (!files[i].delete())
                log.warn("Failed to M-DELETE " + files[i]);
        }        
    }
    
    public void checkForFilesToCompress() throws RemoteException,
            FinderException {
        log.info("Check For Files To Compress on attached filesystems!");
        String cuid;
        Timestamp before;
        CompressionRule info;
        FileDTO[] files;
        int limit = limitNumberOfFilesPerTask;
        String[] fsdirs = getFileSystemDirPaths();
        FileSystemMgt fsMgt = newFileSystemMgt();
        try {
            for (int i = 0, len = compressionRuleList.size(); i < len && limit > 0; i++) {
                info = (CompressionRule) compressionRuleList.get(i);
                cuid = info.getCUID();
                before = new Timestamp(System.currentTimeMillis() - info.getDelay());
                for (int j = 0; j < fsdirs.length; j++) {
                    files = fsMgt.findFilesToCompress(fsdirs[j],
                            info.getCUID(), before, limit);
                    if (files.length > 0) {
                        log.debug("Compress " + files.length + " files on filesystem "
                                + fsdirs[j] + " triggered by " + info);
                        for (int k = 0; k < files.length; k++) {
                            doCompress(fsMgt, files[k], info);
                        }
                        limit -= files.length;
                    }
                }
            }
        } finally {
            try {
                fsMgt.remove();
            } catch (Exception ignore) {}
        }
    }

    private String[] getFileSystemDirPaths() {
        try {
            return (String[]) server.invoke(fileSystemMgtName,
                    "getFileSystemDirPaths", null, null);
        } catch (JMException e) {
            throw new RuntimeException(
                    "Failed to invoke getFileSystemDirPaths", e);
        }
    }

     private void doCompress(FileSystemMgt fsMgt, FileDTO fileDTO,
            CompressionRule info) {
        File baseDir = FileUtils.toFile(fileDTO.getDirectoryPath());
        File srcFile = FileUtils.toFile(fileDTO.getDirectoryPath(), fileDTO
                .getFilePath());
        File destFile = incFileName(srcFile);
        if (log.isDebugEnabled())
            log.debug("Compress file " + srcFile + " to " + destFile
                    + " with CODEC:" + info.getCodec() + "("
                    + info.getTransferSyntax() + ")");
        try {
            int[] pxvalVR = new int[1];
            byte[] md5 = CompressCmd.compressFile(srcFile, destFile, info
                    .getTransferSyntax(), pxvalVR);
            if (verifyCompression) {
                File absTmpDir = FileUtils.resolve(tmpDir);
                if (absTmpDir.mkdirs())
                    log.info("Create directory for decompressed files");
                File decFile = new File(absTmpDir,
                        srcFile.getPath().substring(1).replace(File.separatorChar, '-')
                        + _DCM);
                byte[] dec_md5 = DecompressCmd.decompressFile(destFile,
                        decFile, fileDTO.getFileTsuid(), pxvalVR[0]);
                if (!Arrays.equals(dec_md5, fileDTO.getFileMd5())) {
                    log.info("File MD5 check failed for src file " + srcFile
                            + "! Check pixel matrix now.");
                    if (!FileUtils.equalsPixelData(srcFile, decFile)) {
                        log.warn("Pixel matrix after decompression differs from original file "
                                        + srcFile
                                        + "! Keep original uncompressed file.");
                        destFile.delete();
                        fsMgt.setFileStatus(fileDTO.getPk(),
                                FileDTO.VERIFY_COMPRESS_FAILED);
                        if (keepTempFileIfVerificationFails <= 0L)
                            decFile.delete();
                        return;
                    }
                }
                decFile.delete();
            }
            final int baseDirPathLength = baseDir.getPath().length();
            final String destFilePath = destFile.getPath().substring(
                    baseDirPathLength + 1).replace(File.separatorChar, '/');
            if (log.isDebugEnabled())
                log.debug("replace File " + srcFile + " with " + destFile);
            fsMgt.replaceFile(fileDTO.getPk(), destFilePath, info
                    .getTransferSyntax(), (int) destFile.length(), md5);
        } catch (Exception x) {
            log.error("Can't compress file:" + srcFile, x);
            if (destFile.exists())
                destFile.delete();
            try {
                fsMgt.setFileStatus(fileDTO.getPk(), FileDTO.COMPRESS_FAILED);
            } catch (Exception x1) {
                log.error("Failed to set FAILED_TO_COMPRESS for file "
                        + srcFile);
            }
        }
    }

    private File incFileName(File src) {
        File path = src.getParentFile();
        long fnAsInt = Long.parseLong(src.getName(), 16);
        File f;
        do {
            f = new File(path, Long.toHexString(++fnAsInt).toUpperCase());            
        } while (f.exists());
        return f;
    }

    private boolean isDisabled(int hour) {
        if (disabledEndHour == -1) return false;
        boolean sameday = disabledStartHour <= disabledEndHour;
        boolean inside = hour >= disabledStartHour && hour < disabledEndHour; 
        return sameday ? inside : !inside;
    }

    protected void startService() throws Exception {
        super.startService();
        listenerID = startScheduler("CheckFilesToCompress", taskInterval,
        		delayedCompressionListener);
    }

    protected void stopService() throws Exception {
        stopScheduler("CheckFilesToCompress", listenerID,
        		delayedCompressionListener);
        super.stopService();
    }

    private FileSystemMgt newFileSystemMgt() {
        try {
            FileSystemMgtHome home = (FileSystemMgtHome) EJBHomeFactory
                    .getFactory().lookup(FileSystemMgtHome.class,
                            FileSystemMgtHome.JNDI_NAME);
            return home.create();
        } catch (Exception e) {
            throw new RuntimeException("Failed to access File System Mgt EJB:",
                    e);
        }
    }

    public class CompressionRule {
        String cuid;
        int codec;
        long delay;
        public CompressionRule(String s) {
            String[] a = StringUtils.split(s, ':');
            if (a.length != 3)
                throw new IllegalArgumentException("Wrong format - " + s);
            cuid = a[0];
            getCUID(); // check cuid
            codec = Arrays.asList(CODEC_NAMES).indexOf(a[1]);
            if (codec == -1)
                throw new IllegalArgumentException("Unrecognized codec - " + a[1]);
            delay = RetryIntervalls.parseInterval(a[2]);
        }

        /**
         * @return
         */
        public String getCodec() {
            return CODEC_NAMES[codec];
        }

        /**
         * @return
         */
        public String getCUID() {
            return Character.isDigit(cuid.charAt(0)) ? cuid : UIDs.forName(cuid);
        }

        /**
         * @return Returns the before.
         */
        public long getDelay() {
            return delay;
        }

        /**
         * @return Returns the codec.
         */
        public String getTransferSyntax() {
            return COMPRESS_TRANSFER_SYNTAX[codec];
        }

        public String toString() {
            return cuid + ":" + getCodec() + ':'
                    + RetryIntervalls.formatInterval(delay);
        }
    }
}