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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
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
package de.iftm.dcm4che.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URL;
import java.net.ConnectException;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.text.ParseException;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParseException;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.DictionaryFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDDictionary;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DataSource;
import org.dcm4che.net.FutureRSP;
import org.dcm4che.net.PDU;
import org.dcm4che.net.PresContext;
import org.dcm4che.server.PollDirSrv;
import org.dcm4che.server.PollDirSrvFactory;
import org.dcm4che.util.DcmURL;
import org.dcm4che.util.SSLContextAdapter;

import org.apache.log4j.Logger;


/**
 * Implementation of C-DIMSE services.
 * <p>
 * <p> Usage:
 * <p> 1. Create a new instance of this class.
 * <p> 2. Use the aASSOCIATE method to establish an association.
 * <p> 3. Use the cFIND method to query  the archive.
 * <p> 4. Use the cMOVE method to move an object from the archive to a destination server.
 * <p> 5. Use the cSTORE to store an object into an archive.
 * <p> 6. Use the cECHO to verfy a association.
 * <p> 7. If you are ready with the C-DIMSE services use the aRELEASE method to close the association.
 * <p>
 * <p>Based on dcm4che 1.4.0 sample: MoveStudy.java revision date 2005-10-05
 * <p>Based on dcm4che 1.4.0 sample: DcmSnd.java revision date 2005-10-05
 * <p>
 * <p>See: PS 3.4 - Annex B STORAGE SERVICE CLASS
 * <p>See: PS 3.4 - Annex C QUERY/RETRIEVE SERVICE CLASS
 * <p>See: PS 3.4 - C.6 SOP CLASS DEFINITIONS
 * <p>See: PS 3.4 - C.6.2 Study Root SOP Class Group
 * <p>
 * <p>Details of how to run the services is given in a configuration property file.
 * A sample may be found at "./resources/CDimseService.cfg".
 *
 *
 * @author Thomas Hacklaender
 * @version 2006-06-28
 */
public class CDimseService {
    
    static final Logger log = Logger.getLogger("CDimseService");
    
    /** The DEBUG flag is set, if the logging level of this class is Debug */
    final static boolean DEBUG = log.isDebugEnabled();
    
    //>>>> Factorys >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    private static final UIDDictionary uidDict = DictionaryFactory.getInstance().getDefaultUIDDictionary();
    private static final AssociationFactory aFact = AssociationFactory.getInstance();
    private static final DcmObjectFactory oFact = DcmObjectFactory.getInstance();
    private static final DcmParserFactory pFact = DcmParserFactory.getInstance();
    private final static DcmObjectFactory dof = DcmObjectFactory.getInstance();
    
    /** Default AE title used for the association if it is not explicit given in the url filed. */
    private final static String DEFAULT_CALLING_AET = "MYAET";
    
    /** Query/Retrieve Level Values for Study. See PS 3.4 - C.6 SOP CLASS DEFINITIONS*/
    private final static String STUDY_LEVEL = "STUDY";
    
    /** Query/Retrieve Level Values for Series. See PS 3.4 - C.6 SOP CLASS DEFINITIONS*/
    private final static String SERIES_LEVEL = "SERIES";
    
    /** Query/Retrieve Level Values for Image. See PS 3.4 - C.6 SOP CLASS DEFINITIONS*/
    private final static String IMAGE_LEVEL = "IMAGE";
    
    /** DICOM URL to define a communication partner for an association.
     * <p>PROTOCOL://CALLED[:CALLING]@HOST[:PORT]
     * <p>
     * <p>PROTOCOL Specifies protocol. Possible values:
     * <p>   - dicom            DICOM default (without TLS)
     * <p>   - dicom-tls        DICOM on TLS (offer AES and DES encryption)
     * <p>   - dicom-tls.aes    DICOM on TLS (force AES or DES encryption)
     * <p>   - dicom-tls.3des   DICOM on TLS (force DES encryption)
     * <p>   - dicom-tls.nodes  DICOM on TLS (no encryption)
     * <p>CALLED  Called AET in association request (max 16 chars)
     * <p>CALLING Calling AET in association request (max 16 chars) [default id final field DEFAULT_CALLING_AET = MYAET]
     * <p>HOST    Name or IP address of host, where the server is running
     * <p>PORT    TCP port address, on which the server is listing for
     * <p>        incoming TCP Transport Connection Indication [default=104] */
    private DcmURL url = null;
    
    /** Message priority. Possible values Command.LOW = 2, Command.MEDIUM = 0, Command.HIGH = 1*/
    private int priority = Command.MEDIUM;
    
    /** Time-out waiting [in msec] for A-ASSOCIATE-AC acknowlage, 0 is interpreted
     * as an infinite timeout [default=5000]. */
    private int acTimeout = 5000;
    
    /** Time-out waiting [in msec] for DIMSE on aASSOCIATE association, 0 is
     * interpreted as an infinite timeout [default=0]. */
    private int dimseTimeout = 0;
    
    /** Time delay [in msec] for socket aRELEASE after sending A-ABORT [default=500]. */
    private int soCloseDelay = 500;
    
    /** Association Request package (A-ASSOCIATE-RQ) is part of the connection
     * service ACSE (Association Control Service Element). In TCP/IP networks
     * connection services are emulated by the "DICOM Upper Layer Service".
     * This presentation srvice encapsulats the data in PDUs (Protocol Data Unit). */
    private AAssociateRQ assocRQ = aFact.newAAssociateRQ();
    
    /** Association object for establishing an active association. */
    private Association assoc = null;
    
    /** Accepted association */
    private ActiveAssociation aassoc = null;
    
    /** Activates packing of command PDV (Presentation Data Value) + (first) data
     * PDV into one P-DATA-TF PDU (Protocol Data Unit) */
    private boolean packPDVs = false;
    
    /** TLS context. Set by method initTLS */
    private SSLContextAdapter tls = null;
    
    /** An array of implemented ciphers for the communication protocol (given in url). */
    private String[] cipherSuites = null;
    
    /** Specifies Key Attributes used for C-FIND.
     * Wildcards '*','?', '-' and '\' are allowed as element-values (see PS 3.4 - C.2.2.2 Attribute Matching).
     * If an key attribute is defined, but has an empty value, it matches every value at the storage side.
     * <p>Key attributes have a type (PS 3.4 - C.2.2.1 Attribute Types)
     * <p>  - U = one Attribute shall be defined as a Unique Key.
     * <p>  - R = a set of Attributes shall be defined as Required Keys.
     * <p>  - O = a set of Attributes shall be defined as Optional Keys.
     * <p>The complete list of Key Attributes can be found at PS 3.4 - C.6.2 Study Root SOP Class Group.
     * <p>As a result of C-FIND for each matching item in the archive a DIMSE object containing
     * the Key Attributes is send back. In this object the Key Attributes are set corresponding to
     * values found in the archive. Attributes of type "O" may be send back with empty value.
     * Only used by C-FIND. */
    private Dataset keys = dof.newDataset();
    
    /** Application Entity Title (AET) of the destination for the C-MOVE. The
     * AET must be known by the archive together with the host IP adress and the
     * port number. Only used by C-MOVE. */
    private String dest;
    
    
    //>>>> Constructor >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    
    /**
     *  Constructor for the StorageSCUServiceClass object. Initializes everything.
     *
     * <p>Details of how to run the server is given in another configuration property file.
     * A sample may be found at "./resources/StorageSCUServiceClass.cfg".
     *
     * @param cfg the configuration properties for this class.
     * @param url the DcmURL of the communication partner.
     * @throws ParseException
     */
    public CDimseService(ConfigProperties cfg, DcmURL url) throws ParseException {
        this.url = url;
        this.priority = Integer.parseInt(cfg.getProperty("prior", "0"));
        this.packPDVs = "true".equalsIgnoreCase(cfg.getProperty("pack-pdvs", "false"));
        initAssocParam(cfg, url);
        // initEchoAssocParam(cfg, url);
        initTLS(cfg);
        
        // Only used by C-FIND
        initKeys(cfg);
        
        // Only used by C-MOVE
        this.dest = cfg.getProperty("dest");
    }
    
    
    //>>>> Methods >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    
    /**
     * Initializes Association related parameters.
     *
     * @param cfg the configuration properties for this class.
     * @param url the DcmURL of the communication partner.
     */
    private final void initAssocParam(ConfigProperties cfg, DcmURL url) {
        String callingAET = null;
        
        //>>>> Get data for filling the Association object for establishing an
        //>>>> active association from configuration file
        
        acTimeout = Integer.parseInt(cfg.getProperty("ac-timeout", "5000"));
        dimseTimeout = Integer.parseInt(cfg.getProperty("dimse-timeout", "0"));
        soCloseDelay = Integer.parseInt(cfg.getProperty("so-close-delay", "500"));
        
        //>>>> Fill the Association Request package (A-ASSOCIATE-RQ)
        
        // Identifying the communication partner by an AET (Application Entity Title)
        assocRQ.setCalledAET(url.getCalledAET());
        
        // Identifying ourselves by an AET (Application Entity Title)
        if (url.getCallingAET() != null) {
            callingAET = url.getCallingAET();
        } else {
            callingAET = DEFAULT_CALLING_AET;
        }
        assocRQ.setCallingAET(callingAET);
        
        // Maximum size of one PDU (Protocol Data Unit)
        assocRQ.setMaxPDULength(Integer.parseInt(cfg.getProperty("max-pdu-len", "16352")));
        
        // Defines possibilities for asynchron DIMSE communication. Noramly synchron DIMSE communication is used.
        // API doc: AssociationFactory.newAsyncOpsWindow(int maxOpsInvoked, int maxOpsPerfomed)
        // PS 3.7 - Annex D.3.3.3 ASYNCHRONOUS OPERATIONS WINDOW NEGOTIATION
        // maxOpsInvoked: This field shall contain the Maximum-number-operationsinvoked as defined for the Association-requester
        // maxOpsPerfomed: This field shall contain the Maximum-number-operationsperformed as defined for the Association-requester
        assocRQ.setAsyncOpsWindow(aFact.newAsyncOpsWindow(Integer.parseInt(cfg.getProperty("max-op-invoked", "0")), 1));
        
        for (Enumeration it = cfg.keys(); it.hasMoreElements();) {
            String key = (String) it.nextElement();
            
            // Setup available transfer syntaces for storage SOP classes
            // PS 3.4 - Annex B STORAGE SERVICE CLASS
            // PS 3.4 - B.5 STANDARD SOP CLASSES
            if (key.startsWith("pc.")) {
                initPresContext(Integer.parseInt(key.substring(3)), cfg.tokenize(cfg.getProperty(key), new LinkedList()));
            }
        }
    }
    
    
    /**
     * Only used by method initAssocParam:
     * Sets up available transfer syntaces for storage SOP classes.
     * @param pcid is a for the association unique odd number between 1 and 255.
     * @param val a list: First element is the symbolic name of the UID of the
     *            SOP to transmit, the following elements are the supportet
     *            transfer syntax for that SOP.
     */
    private final void initPresContext(int pcid, List val) {
        Iterator it = val.iterator();
        String as = UIDs.forName((String) it.next());
        String[] tsUIDs = new String[val.size() - 1];
        for (int i = 0; i < tsUIDs.length; ++i) {
            tsUIDs[i] = UIDs.forName((String) it.next());
        }
        // API doc: AssociationFactory.newPresContext(int pcid, String asuid, String[] tsuid)
        // pcid is a for the association unique odd number between 1 and 255.
        // asuid is the UID of a SOP class
        // TS list of transfer syntaces supported by this class for asuid.
        assocRQ.addPresContext(aFact.newPresContext(pcid, as, tsUIDs));
    }
    
    
    /**
     * Initializes TLS (Transport Layer Security, predecessor of SSL, Secure 
     * Sockets Layer) connection related parameters. TLS expects RSA (Ron Rivest, 
     * Adi Shamir and Len Adleman) encoded keys and certificates.
     * <p>Keys and certificates may be stored in PKCS #12 (Public Key 
     * Cryptography Standards) or JKS (Java Keystore) containers.
     *
     * @param cfg the configuration properties for this class.
     * @throws ParseException
     */
    private void initTLS(ConfigProperties cfg) throws ParseException {
        char[]  keystorepasswd;
        char[]  keypasswd;
        char[]  cacertspasswd;
        URL     keyURL;
        URL     cacertURL;
        String  value;
        
        try {
            
            // Cipher suites for protokoll:
            // dicom = null
            // dicom-tls = SSL_RSA_WITH_NULL_SHA, TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA
            // dicom-tls.3des = SSL_RSA_WITH_3DES_EDE_CBC_SHA
            // dicom-tls.aes = TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA
            // dicom-tls.nodes = SSL_RSA_WITH_NULL_SHA
            cipherSuites = url.getCipherSuites();
            if (cipherSuites == null) {
                return;
            }
            
            // Get a new TLS context
            tls = SSLContextAdapter.getInstance();
            
            //>>>> Managing the keystore file containing the privat key and
            //>>>> public certificate to establish the communication
            
            // Password for keystore [default: secret]
            keystorepasswd = cfg.getProperty("tls-keystore-passwd", "secret").toCharArray();
            
            // Password the private key [default: secret]
            keypasswd = cfg.getProperty("tls-key-passwd", "secret").toCharArray();
            
            // URL of the file containing the keystore
            keyURL = CDimseService.class.getResource("resources/identity.p12");
            
            // If availabel, replace URL with the one specified in the configuration file
            if ((value = cfg.getProperty("tls-key")) != null) {
                try {
                    // Property specified, try to set to specified value
                    keyURL = ConfigProperties.fileRefToURL(CDimseService.class.getResource(""), value);
                } catch (Exception e) {
                    log.warn("Wrong value for tls-key: " + value + ". tls-key was set to default value.");
                }
            }
            
            // log.info("Key URL: " + keyURL.toString());
            
            // Sets the key attribute of the SSLContextAdapter object
            // API doc: SSLContextAdapter.loadKeyStore(java.net.URL url, char[] password)
            // API doc: SSLContextAdapter.setKey(java.security.KeyStore key, char[] password)
            tls.setKey(tls.loadKeyStore(keyURL, keystorepasswd), keypasswd);
            
            //>>>> Managing the keystore containing the public certificates of the Ceritifying
            //>>>> Authorities used for signing the public certificates of the public keys
            
            // Get the password of the keystore
            cacertspasswd = cfg.getProperty("tls-cacerts-passwd", "secret").toCharArray();
            
            // URL of the file containing the keystore
            cacertURL = CDimseService.class.getResource("resources/cacerts.jks");
            
            // If availabel, replace URL with the one specified in the configuration file
            if ((value = cfg.getProperty("tls-cacerts")) != null) {
                try {
                    // Property specified, try to set to specified value
                    cacertURL = ConfigProperties.fileRefToURL(CDimseService.class.getResource(""), value);
                } catch (Exception e) {
                    log.warn("Wrong value for tls-cacerts: " + value + ". tls-cacerts was set to default value.");
                }
            }
            
            // log.info("Trust certificate URL: " + cacertURL.toString());
            
            // Sets the trust attribute of the SSLContextAdapter object
            // API doc: SSLContextAdapter.loadKeyStore(java.net.URL url, char[] password)
            // API doc: SSLContextAdapter.setTrust(java.security.KeyStore cacerts)
            tls.setTrust(tls.loadKeyStore(cacertURL, cacertspasswd));
            
            // Init TLS context adapter
            tls.init();
            
        } catch (Exception ex) {
            throw new ParseException("Could not initalize TLS configuration.", 0);
        }
    }
    
    
    /**
     * Prepares the Dataset representing the search key in C-FIND. As no values are
     * set, the keys match to every content in the archive. The user has to specify
     * concret values to limit the searchSee PS 3.4 - C.6.2.1.2 Study level.
     * <p>As the result for C-FIND these keys are filled with the values found in the archive.
     * @param cfg the configuration properties for this class.
     * @throws ParseException if a given properties for the keys was not found.
     */
    private void initKeys(ConfigProperties cfg) throws ParseException {
        
        // Query/Retrieve Level. PS 3.4 - C.6.2 Study Root SOP Class Group
        keys.putCS(Tags.QueryRetrieveLevel, STUDY_LEVEL);
        
        // UNIQUE STUDY LEVEL KEY FOR THE STUDY. See PS 3.4 - C.6.2.1.2 Study level
        keys.putUI(Tags.StudyInstanceUID);
        
        // REQUIRED STUDY LEVEL KEY FOR THE STUDY. See PS 3.4 - C.6.2.1.2 Study level
        keys.putDA(Tags.StudyDate);
        // Not defined: StudyTime
        // Not defined: AccessionNumber
        keys.putPN(Tags.PatientName);
        keys.putLO(Tags.PatientID);
        // Not defined: StudyID
        
        // OPTIONAL STUDY LEVEL KEY FOR THE STUDY. See PS 3.4 - C.6.2.1.2 Study level
        keys.putUS(Tags.NumberOfStudyRelatedSeries);
        keys.putUS(Tags.NumberOfStudyRelatedInstances);
        // mutch more defined...
        
        // Add/replace keys found in the configuration file. Syntax key.<element name> = <element value>
        for (Enumeration it = cfg.keys(); it.hasMoreElements(); ) {
            String key = (String) it.nextElement();
            if (key.startsWith("key.")) {
                try {
                    keys.putXX(Tags.forName(key.substring(4)), cfg.getProperty(key));
                } catch (Exception e) {
                    throw new ParseException("Illegal entry in configuration filr: " + key + "=" + cfg.getProperty(key), 0);
                }
            }
        }
    }
    
    
    /**
     * Starts a active association to a communication partner.
     * See PS 3.8 - 7.1 A-ASSOCIATE SERVICE
     *
     * @return true, if association was successful established.
     * @exception  ConnectException
     * @exception  IOException
     * @exception  GeneralSecurityException
     */
    public boolean aASSOCIATE() throws ConnectException, IOException, GeneralSecurityException {
        
        // No association may be active
        if (assoc != null) {
            throw new ConnectException("Association already established");
        }
        
        // New Association object for establishing an active association
        assoc = aFact.newRequestor(newSocket(url.getHost(), url.getPort()));
        
        //>>>> Fill the Association object with relevant data
        assoc.setAcTimeout(acTimeout);
        assoc.setDimseTimeout(dimseTimeout);
        assoc.setSoCloseDelay(soCloseDelay);
        assoc.setPackPDVs(packPDVs);
        
        // 1. Create an communication channel to the communication partner defined in the Association object
        // 2. Send the A-ASSOCIATE-RQ package
        // 3. Receive the aAssociation acknowlage/reject package from the communication partner as a PDU (Protocol Data Unit)
        PDU assocAC = assoc.connect(assocRQ);
        
        if (!(assocAC instanceof AAssociateAC)) {
            // Acknowlage is A-ASSOCIATE-RJ
            // Association rejected
            assoc = null;
            
            // Return aASSOCIATE faild
            return false;
        }
        // Acknowlage is A-ASSOCIATE_AC
        // Association accepted
        
        // Start the accepted association
        // API doc: AssociationFactory.newActiveAssociation(Association assoc, DcmServiceRegistry services)
        aassoc = aFact.newActiveAssociation(assoc, null);
        aassoc.start();
        
        // Return successfull opened
        return true;
    }
    
    
    /**
     * Creates a stream socket and connects it to the specified port number on the named host.
     * @param host the IP address.
     * @param port the port number.
     * @return the Socket.
     * @exception  IOException
     * @exception  GeneralSecurityException
     */
    private Socket newSocket(String host, int port) throws IOException, GeneralSecurityException {
        
        // Test, if a secured connection is needed
        if (cipherSuites != null) {
            
            // Creates a socket for secured connection.
            // The SSLContextAdapter tls uses the javax.net.ssl package for establishing
            // the connection.
            return tls.getSocketFactory(cipherSuites).createSocket(host, port);
        } else {
            
            // Creates a standard Java socket for unsecured connection.
            return new Socket(host, port);
        }
    }
    
    
    /**
     * Releases the active association.
     * See PS 3.8 - 7.2 A-RELEASE SERVICE
     *
     * @param waitOnRSP if true, method waits until it receives the responds 
     *                  to the release request.
     * @exception  InterruptedException  Description of the Exception
     * @exception  IOException           Description of the Exception
     */
    public void aRELEASE(boolean waitOnRSP) throws InterruptedException, IOException {
        if (assoc != null) {
            try {
                aassoc.release(waitOnRSP);
            } finally {
                assoc = null;
                aassoc = null;
            }
        }
    }
    
    
    /**
     * Stores a DICOM object in an archive (Storage SCP).
     * <p>See PS 3.4 - Annex B STORAGE SERVICE CLASS.
     *
     * @param ds the Dataset to store.
     * @throws ConnectException
     * @throws ParseException
     * @throws IOException
     * @throws InterruptedException
     * @throws IllegalStateException
     */
    public void cSTORE(Dataset ds) throws InterruptedException, IOException, ConnectException, ParseException {
        String  sopClassUID;
        String  sopInstUID;
        PresContext pc = null;
        
        // An association must be active
        if (aassoc == null) {
            throw new ConnectException("No Association established");
        }
        
        // SOP Class UID must be given
        if ((sopClassUID = ds.getString(Tags.SOPClassUID)) == null) {
            throw new ParseException("No SOP Class UID in Dataset", 0);
        }
        
        // SOP Instance UID must be given
        if ((sopInstUID = ds.getString(Tags.SOPInstanceUID)) == null) {
            throw new ParseException("No SOP Instance UID in Dataset", 0);
        }
        
        // Test, if applicable presentation context was found
        if ((pc = aassoc.getAssociation().getAcceptedPresContext(sopClassUID, UIDs.ImplicitVRLittleEndian)) == null &&
                (pc = aassoc.getAssociation().getAcceptedPresContext(sopClassUID, UIDs.ExplicitVRLittleEndian)) == null &&
                (pc = aassoc.getAssociation().getAcceptedPresContext(sopClassUID, UIDs.ExplicitVRBigEndian)) == null) {
            
            throw new ConnectException("No applicable presentation context found");
        }
        
        // New Cammand Set, see: DICOM Part 7: Message Exchange, 6.3.1 Command Set Structure
        Command cStoreRQ = oFact.newCommand();
        // API doc: Command.initCStoreRQ(int msgID, String sopClassUID, String sopInstUID, int priority)
        cStoreRQ.initCStoreRQ(assoc.nextMsgID(), sopClassUID, sopInstUID, priority);
        
        // API doc: AssociationFactorynewDimse(int pcid, Command cmd, Dataset ds)
        // DIMSE (DICOM Message Service Element) ist ein Nachrichtendienst in DICOM
        Dimse storeRq = aFact.newDimse(pc.pcid(), cStoreRQ, ds);
        
        // PS 3.7 - 9.3.1 C-STORE PROTOCOL, 9.3.1.2 C-STORE-RSP
        // Always returns SUCESS result code.
        // Invoke active association with echo request Dimse
        FutureRSP future = aassoc.invoke(storeRq);
        
        // Response to the C-ECHO request.
        // The result cannot be accessed until it has been set.
        Dimse storeRsp = future.get();
        Command rspCmd = storeRsp.getCommand();
        
        // PS 3.7 - 9.3.5 C-MOVE PROTOCOL, 9.3.5.2 C-ECHO-RSP
        int status = rspCmd.getStatus();
        switch (status) {
            case 0x0000:
                // Success
                break;
            default:
                log.error("C-STORE failed: " + Integer.toHexString(status));
                break;
        }
        
    }
    
    
    /**
     * Queries the archive for DICOM objects matching Attribute Keys.
     * See PS 3.4 - Annex C QUERY/RETRIEVE SERVICE CLASS.
     * <p>The method returns, when the result is received from the communication partner.
     *
     * @return the result of the cFIND as a Vector of Dataset objects each specifying
     *         one matching DICOM object. If no matching objects are found an empty Vector is returned.
     * @throws ConnectException
     * @throws IOException
     */
    public Vector cFIND() throws ConnectException, IOException, InterruptedException {
        PresContext pc;
        List dimseList;
        Vector datasetVector;
        
        // An association must be active
        if (aassoc == null) {
            throw new ConnectException("No Association established");
        }
        
        // Test, if Presentation Context for C-FIND is supported
        // API doc: Association.getAcceptedPresContext(String asuid, String tsuid)
        if ((pc = aassoc.getAssociation().getAcceptedPresContext(UIDs.StudyRootQueryRetrieveInformationModelFIND, UIDs.ExplicitVRLittleEndian)) == null &&
                (pc = aassoc.getAssociation().getAcceptedPresContext(UIDs.StudyRootQueryRetrieveInformationModelFIND, UIDs.ImplicitVRLittleEndian)) == null) {
            throw new ConnectException("Association does not support presentation context for StudyRootQueryRetrieveInformationModelFIND SOP.");
        }
        
        // New Cammand Set, see: DICOM Part 7: Message Exchange, 6.3.1 Command Set Structure
        Command rqCmd = dof.newCommand();
        // API doc: Command.initCFindRQ(int msgID, String sopClassUID, int priority)
        rqCmd.initCFindRQ(assoc.nextMsgID(), UIDs.StudyRootQueryRetrieveInformationModelFIND, priority);
        // API doc: AssociationFactorynewDimse(int pcid, Command cmd, Dataset ds)
        // DIMSE (DICOM Message Service Element) ist ein Nachrichtendienst in DICOM
        Dimse findRq = aFact.newDimse(pc.pcid(), rqCmd, keys);
        
        if (DEBUG) {
            StringWriter w = new StringWriter();
            w.write("C-FIND RQ Identifier:\n");
            keys.dumpDataset(w, null);
            log.debug(w.toString());
        }
        
        // Invoke active association with find request Dimse
        FutureRSP future = aassoc.invoke(findRq);
        // Response to the C-FIND request.
        // The result cannot be accessed until it has been set.
        Dimse findRsp = future.get();
        
        // Get the list of found objects
        dimseList = future.listPending();
        
        //>>>> Extract Dataset from Dimse
        
        datasetVector = new Vector();
        
        // If no List of DIMSE objects was generated or it is empty return an empty Vector
        if (dimseList == null || dimseList.isEmpty()) {
            return datasetVector;
        }
        
        // Process all elements
        for (int i = 0; i < dimseList.size(); i++) {
            datasetVector.addElement(((Dimse) dimseList.get(i)).getDataset());
        }
        
        return datasetVector;
    }
    
    
    /**
     * Ask archive to move one DICOM object to the destination (C-MOVE).
     * See PS 3.4 - Annex C QUERY/RETRIEVE SERVICE CLASS.
     * <p>Use the Study Root Query/Retrieve Information Model to communicate with the archive.
     * See PS 3.4 - C.6.2 Study Root SOP Class Group.
     *
     * @param ds the DICOM object represented as a Dataset.
     * @return a result-code: 0x0000 = SUCCESS sub-operations complete �no failures,
     *                        0xB000 = WARNING sub-operations complete � one or more failures,
     *                        other = errors defined in PS 3.4 - C.4.2.1.5 Status
     * @throws ConnectException
     * @throws InterruptedException
     * @throws IOException
     */
    public int cMOVE(Dataset ds) throws ConnectException, InterruptedException, IOException {
        PresContext pc;
        
        // An association must be active
        if (aassoc == null) {
            throw new ConnectException("No Association established");
        }
        
        // Test, if Presentation Context for C-MOVE is supported
        // API doc: Association.getAcceptedPresContext(String asuid, String tsuid)
        if ((pc = aassoc.getAssociation().getAcceptedPresContext(UIDs.StudyRootQueryRetrieveInformationModelMOVE, UIDs.ExplicitVRLittleEndian)) == null &&
                (pc = aassoc.getAssociation().getAcceptedPresContext(UIDs.StudyRootQueryRetrieveInformationModelMOVE, UIDs.ImplicitVRLittleEndian)) == null) {
            throw new ConnectException("Association does not support presentation context for StudyRootQueryRetrieveInformationModelMOVE SOP.");
        }
        
        // Get the Study Instance UID of the study to mode
        String suid = ds.getString(Tags.StudyInstanceUID);
        
        // Prepare info for logging
        String patName = ds.getString(Tags.PatientName);
        String patID = ds.getString(Tags.PatientID);
        String studyDate = ds.getString(Tags.StudyDate);
        String prompt = "Study[" + suid + "] from " + studyDate + " for Patient[" + patID + "]: " + patName;
        
        // log.info("Moving: " + prompt);
        
        // New Cammand Set, see: DICOM Part 7: Message Exchange, 6.3.1 Command Set Structure
        Command rqCmd = dof.newCommand();
        // API doc: Command.initCMoveRQ(int msgID, String sopClassUID, int priority, String moveDest)
        rqCmd.initCMoveRQ(assoc.nextMsgID(), UIDs.StudyRootQueryRetrieveInformationModelMOVE, priority, dest);
        Dataset rqDs = dof.newDataset();
        rqDs.putCS(Tags.QueryRetrieveLevel, STUDY_LEVEL);
        // Only Unique Key allowed in C-MOVE. PS 3.4 -C.2.2.1 Attribute Types
        rqDs.putUI(Tags.StudyInstanceUID, suid);
        // API doc: AssociationFactorynewDimse(int pcid, Command cmd, Dataset ds)
        // DIMSE (DICOM Message Service Element) ist ein Nachrichtendienst in DICOM
        Dimse moveRq = aFact.newDimse(pc.pcid(), rqCmd, rqDs);
        
        // Invoke active association with move request Dimse
        FutureRSP future = aassoc.invoke(moveRq);
        // Response to the C-MOVE request.
        // The result cannot be accessed until it has been set.
        Dimse moveRsp = future.get();
        Command rspCmd = moveRsp.getCommand();
        
        // PS 3.7 - 9.3.4 C-MOVE PROTOCOL, 9.3.4.2 C-MOVE-RSP
        int status = rspCmd.getStatus();
        switch (status) {
            case 0x0000:
                // log.info("Moved: " + prompt);
                break;
            case 0xB000:
                log.error("One or more failures during move of " + prompt);
                break;
            default:
                log.error("Failed to move " + prompt + "\n\terror tstatus: " + Integer.toHexString(status));
                break;
        }
        return status;
    }
    
    
    /**
     * Implements the ECHO service. The C-ECHO service is invoked by a DIMSE-service-user
     * to verify end-to-end communications with a peer DIMSE-service-user.
     * See PS 3.7 - 9.1.5 C-ECHO SERVICE
     *
     * @exception ConnectException
     * @exception InterruptedException
     * @exception IOException
     */
    public long cECHO() throws ConnectException, InterruptedException, IOException {
        PresContext pc;
        long t1 = System.currentTimeMillis();
        
        // An association must be active
        if (aassoc == null) {
            throw new ConnectException("No Association established");
        }
        
        // Test, if Presentation Context for C-ECHO is supported
        // API doc: Association.getAcceptedPresContext(String asuid, String tsuid)
        if ((pc = aassoc.getAssociation().getAcceptedPresContext(UIDs.Verification, UIDs.ImplicitVRLittleEndian)) == null) {
            throw new ConnectException("Association does not support presentation context: Verification SOP/ImplicitVRLittleEndian.");
        }
        
        // New Cammand Set, see: DICOM Part 7: Message Exchange, 6.3.1 Command Set Structure
        Command cEchoRQ = oFact.newCommand();
        // API doc: Command.initCEchoRQ(int msgID)
        cEchoRQ.initCEchoRQ(1);
        
        // API doc: AssociationFactorynewDimse(int pcid, Command cmd)
        // DIMSE (DICOM Message Service Element) ist ein Nachrichtendienst in DICOM
        Dimse echoRq = aFact.newDimse(pc.pcid(), cEchoRQ);
        
        // PS 3.7 - 9.3.5 C-ECHO PROTOCOL, 9.3.5.2 C-ECHO-RSP
        // Always returns SUCESS result code.
        // Invoke active association with echo request Dimse
        FutureRSP future = aassoc.invoke(echoRq);
        
        // Response to the C-ECHO request.
        // The result cannot be accessed until it has been set.
        Dimse echoRsp = future.get();
        Command rspCmd = echoRsp.getCommand();
        
        // PS 3.7 - 9.3.5 C-MOVE PROTOCOL, 9.3.5.2 C-ECHO-RSP
        int status = rspCmd.getStatus();
        switch (status) {
            case 0x0000:
                // Success
                break;
            default:
                log.error("C-ECHO failed: " + Integer.toHexString(status));
                break;
        }
        
        return System.currentTimeMillis() - t1;
    }
    
}
