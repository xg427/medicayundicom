/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/
package org.dcm4chex.archive.ejb.session;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 1559 $ $Date: 2005-02-25 23:20:31 +0800 (周五, 25 2月 2005) $
 */
public class StorageBeanTest extends TestCase {

    public static final String CALLING_AET = "STORE_SCU";
    public static final String CALLED_AET = "STORE_SCP";
    public static final String RETRIEVE_AET = "QR_SCP";
    public static final String DIR = "storage";
    public static final String AET = "StorageBeanTest";
    public static final DcmObjectFactory objFact =
        DcmObjectFactory.getInstance();

    private Storage storage;
 
    public static void main(String[] args) {
        junit.textui.TestRunner.run(StorageBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        Context ctx = new InitialContext();
        StorageHome home = (StorageHome) ctx.lookup(StorageHome.JNDI_NAME);
        ctx.close();
        storage = home.create();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        storage.remove();
    }

    /**
     * Constructor for StudyBeanTest.
     * @param name
     */
    public StorageBeanTest(String name) {
        super(name);
    }

    public void testStore() throws Exception {
        doStore("", new File(DIR));
    }

    private void doStore(String path, File file) throws Exception {
        if (file.isDirectory()) {
            File[] f = file.listFiles();
            for (int i = 0; i < f.length; i++) {
                doStore(path + '/' + f[i].getName(), f[i]);
            }
            return;
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        Dataset ds = loadDataset(file, md);
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ds.putAE(PrivateTags.CallingAET, CALLING_AET);
        ds.putAE(PrivateTags.CalledAET, CALLED_AET);
        ds.putAE(Tags.RetrieveAET, RETRIEVE_AET);        
        storage.store(ds, "/", path.substring(1), (int) file.length(),
        		md.digest());
    }

    private Dataset loadDataset(File file, MessageDigest md)
        throws IOException {
        InputStream is = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(is);
        DigestInputStream dis = new DigestInputStream(is, md);
        Dataset ds = objFact.newDataset();
        try {
            ds.readFile(dis, FileFormat.DICOM_FILE, -1);
        } finally {
            try {
                dis.close();
            } catch (IOException ignore) {
            }
        }
        if (ds.getFileMetaInfo() == null) {
        }
        ds.remove(Tags.PixelData);
        return ds;
    }

}
