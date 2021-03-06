/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/
package org.dcm4chex.cdw.common;

import java.io.File;
import java.io.IOException;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4164 $ $Date: 2004-07-22 21:26:59 +0800 (周四, 22 7月 2004) $
 * @since 28.06.2004
 */
public class FileUtils {

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private FileUtils() {}
    
    public static Dataset readDataset(File f, Logger log) throws IOException {
        if (log.isDebugEnabled())
            log.debug("M-READ " + f);
        Dataset ds = dof.newDataset();
        try {
            ds.readFile(f, FileFormat.DICOM_FILE, Tags.PixelData);
        } catch (IOException e) {
            log.error("Failed: M-READ " + f, e);
            throw e;
        }
        return ds;
    }

    public static void writeDataset(Dataset ds, File f, Logger log)
        throws IOException {
        if (log.isDebugEnabled())
            log.debug("M-UPDATE " + f);
        try {
            ds.writeFile(f, null);
        } catch (IOException e) {
            log.error("Failed M-UPDATE " + f);
            throw e;
        }
    }

    public static boolean delete(File f, Logger log) {
        if (!f.exists())
            return false;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++)
                delete(files[i], log);
        }
        log.debug("M-DELETE " + f);
        boolean success = f.delete();
        if (!success)
            log.warn("Failed M-DELETE " + f);
        return success;
    }
    
    public static void purgeDir(File d, Logger log) {
        if (d.isDirectory() && d.list().length == 0) {
            log.debug("M-DELETE " + d);
            if (!d.delete())
                log.warn("Failed M-DELETE " + d);
            else
                purgeDir(d.getParentFile(), log);
        }
    };
}
