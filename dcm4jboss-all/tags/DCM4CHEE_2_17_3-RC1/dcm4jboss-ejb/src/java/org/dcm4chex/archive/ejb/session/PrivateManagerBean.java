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

package org.dcm4chex.archive.ejb.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.PatientMatching;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.MPPSLocal;
import org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PrivateFileLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PrivateInstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivateInstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PrivatePatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivatePatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PrivateSeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivateSeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PrivateStudyLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivateStudyLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 17094 $ $Date: 2012-09-11 03:16:06 +0800 (周二, 11 9月 2012) $
 * @since 27.12.2005
 * 
 * @ejb.bean name="PrivateManager" type="Stateless" view-type="remote"
 *           jndi-name="ejb/PrivateManager"
 * 
 * @ejb.transaction-type type="Container"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * 
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study"
 * 
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * 
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 * 
 * @ejb.ejb-ref ejb-name="File" view-type="local" ref-name="ejb/File"
 * 
 * @ejb.ejb-ref ejb-name="PrivatePatient" view-type="local"
 *              ref-name="ejb/PrivatePatient"
 * 
 * @ejb.ejb-ref ejb-name="PrivateStudy" view-type="local"
 *              ref-name="ejb/PrivateStudy"
 * 
 * @ejb.ejb-ref ejb-name="PrivateSeries" view-type="local"
 *              ref-name="ejb/PrivateSeries"
 * 
 * @ejb.ejb-ref ejb-name="PrivateInstance" view-type="local"
 *              ref-name="ejb/PrivateInstance"
 * 
 * @ejb.ejb-ref ejb-name="PrivateFile" view-type="local"
 *              ref-name="ejb/PrivateFile"
 * 
 */
public abstract class PrivateManagerBean implements SessionBean {

    private static final int DELETED = 1;

    private PatientLocalHome patHome;
    private StudyLocalHome studyHome;
    private SeriesLocalHome seriesHome;
    private InstanceLocalHome instHome;

    private PrivatePatientLocalHome privPatHome;
    private PrivateStudyLocalHome privStudyHome;
    private PrivateSeriesLocalHome privSeriesHome;
    private PrivateInstanceLocalHome privInstHome;
    private PrivateFileLocalHome privFileHome;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private static Logger log = Logger.getLogger(PrivateManagerBean.class
            .getName());

    public void setSessionContext(SessionContext arg0) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Patient");
            studyHome = (StudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Study");
            seriesHome = (SeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Series");
            instHome = (InstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Instance");

            privPatHome = (PrivatePatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivatePatient");
            privStudyHome = (PrivateStudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivateStudy");
            privSeriesHome = (PrivateSeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivateSeries");
            privInstHome = (PrivateInstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivateInstance");
            privFileHome = (PrivateFileLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivateFile");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    public void unsetSessionContext() {
        patHome = null;
        studyHome = null;
        seriesHome = null;
        instHome = null;
        privPatHome = null;
        privStudyHome = null;
        privSeriesHome = null;
        privInstHome = null;
        privFileHome = null;
    }

    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public void deletePrivateSeries(long series_pk) throws RemoteException,
            FinderException {
        try {
            PrivateSeriesLocal series = privSeriesHome
                    .findByPrimaryKey(new Long(series_pk));
            PrivateStudyLocal study = series.getStudy();
            series.remove();
            if (study.getSeries().isEmpty()) {
                PrivatePatientLocal pat = study.getPatient();
                study.remove();
                if (pat.getStudies().isEmpty()) {
                    pat.remove();
                }
            }
        } catch (EJBException e) {
            throw new RemoteException("Can't delete series with pk " + series_pk, e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete series with pk " + series_pk, e);
        }
    }

    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public Collection deletePrivateStudy(long study_pk) throws RemoteException,
            FinderException {
        try {
            PrivateStudyLocal study = privStudyHome.findByPrimaryKey(new Long(
                    study_pk));
            ArrayList files = null;
            PrivatePatientLocal pat = study.getPatient();
            study.remove();
            if (pat.getStudies().isEmpty()) {
                pat.remove();
            }
            return files;
        } catch (EJBException e) {
            throw new RemoteException("Can't delete study with pk " + study_pk, e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete study with pk " + study_pk, e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public void deletePrivatePatient(long patient_pk) throws RemoteException {
        try {
            privPatHome.remove(new Long(patient_pk));
        } catch (EJBException e) {
            throw new RemoteException("Can't delete patient with pk " + patient_pk, e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete patient with pk " + patient_pk, e);
        }
    }

    /**
     * @throws FinderException
     * @throws FinderException
     * @ejb.interface-method
     */
    public void deletePrivateInstance(long instance_pk) throws RemoteException,
            FinderException {
        try {
            PrivateInstanceLocal instance = privInstHome
                    .findByPrimaryKey(new Long(instance_pk));
            PrivateSeriesLocal series = instance.getSeries();
            instance.remove();
            if (series.getInstances().isEmpty()) {
                PrivateStudyLocal study = series.getStudy();
                series.remove();
                if (study.getSeries().isEmpty()) {
                    PrivatePatientLocal pat = study.getPatient();
                    study.remove();
                    if (pat.getStudies().isEmpty()) {
                        pat.remove();
                    }
                }
            }
        } catch (EJBException e) {
            throw new RemoteException("Can't delete instance with pk " + instance_pk, e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete instance with pk " + instance_pk, e);
        }
    }

    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public void deletePrivateFile(long file_pk) throws RemoteException {
        try {
            privFileHome.remove(new Long(file_pk));
        } catch (EJBException e) {
            throw new RemoteException("Can't delete file with pk " + file_pk, e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete file with pk " + file_pk, e);
        }
    }

    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public void deletePrivateFiles(Collection fileDTOs) throws RemoteException {
            for (Iterator iter = fileDTOs.iterator(); iter.hasNext();) {
    		deletePrivateFile(new Long(((FileDTO) iter.next()).getPk()));
        }
    }

    /**
     * @ejb.interface-method
     */
    public void deleteAll(int privateType) throws RemoteException {
        try {
            Collection c = privPatHome.findByPrivateType(privateType);
            for (Iterator iter = c.iterator(); iter.hasNext();) {
            	deletePrivatePatient(((PrivatePatientLocal) iter.next()).getPk());
            }
        } catch (FinderException e) {
            throw new RemoteException("Can't find patients with type " + privateType, e);
        }
    }

    /**
     * Delete a list of instances, i.e., move them to trash bin
     * 
     * @ejb.interface-method
     * 
     * @param iuids
     *            A list of instance uid
     * @param cascading
     *            True to delete the series/study if there's no instance/series
     * @return a collection of Dataset containing the actuall detetion
     *         information per study
     * @throws RemoteException
     */
    public Collection moveInstancesToTrash(String[] iuids, boolean cascading)
            throws RemoteException {
        try {
            // These instances may belong to multiple studies,
            // although mostly they should be the same study
            Map mapStudies = new HashMap();
            for (int i = 0; i < iuids.length; i++) {
                InstanceLocal instance = instHome.findBySopIuid(iuids[i]);
                SeriesLocal series = instance.getSeries();
                StudyLocal study = series.getStudy();
                if (!mapStudies.containsKey(study))
                    mapStudies.put(study, new HashMap());
                Map mapSeries = (Map) mapStudies.get(study);
                if (!mapSeries.containsKey(series))
                    mapSeries.put(series, new ArrayList());
                Collection colInstances = (Collection) mapSeries.get(series);
                colInstances.add(instance);
            }

            List dss = new ArrayList();
            Iterator iter = mapStudies.keySet().iterator();
            while (iter.hasNext()) {
                StudyLocal study = (StudyLocal) iter.next();
                dss.add(makeIAN(study, (Map) mapStudies.get(study)));
                Iterator iter2 = ((Map) mapStudies.get(study)).keySet()
                        .iterator();
                while (iter2.hasNext()) {
                    SeriesLocal series = (SeriesLocal) iter2.next();
                    List instances = (List) ((Map) mapStudies.get(study))
                            .get(series);
                    for (int i = 0; i < instances.size(); i++) {
                        // Delete the instance now, i.e., move to trash bin,
                        // becoming private instance
                        getPrivateInstance((InstanceLocal) instances.get(i),
                                DELETED, null);
                        ((InstanceLocal) instances.get(i)).remove();
                    }
                    if (series.getInstances().size() == 0 && cascading) {
                        // Delete the series too since there's no instance left
                        getPrivateSeries(series, DELETED, null, false);
                        series.remove();
                    } else
                        UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series);
                }
                if (study.getSeries().size() == 0 && cascading) {
                    // Delete the study too since there's no series left
                    getPrivateStudy(study, DELETED, null, false);
                    study.remove();
                } else
                    UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
            }

            return dss;
        } catch (CreateException e) {
            throw new RemoteException("Can't delete instances, first instance is " + iuids[0] , e);
        } catch (EJBException e) {
            throw new RemoteException("Can't delete instances, first instance is " + iuids[0] , e);
        } catch (FinderException e) {
            throw new RemoteException("Can't delete instances, first instance is " + iuids[0] , e);
        } catch (RemoveException e) {
            throw new RemoteException("Can't delete instances, first instance is " + iuids[0] , e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset moveInstanceToTrash(long instance_pk) throws RemoteException {
        try {
            InstanceLocal instance = instHome.findByPrimaryKey(new Long(
                    instance_pk));
            Collection colInstance = new ArrayList();
            colInstance.add(instance);
            SeriesLocal series = instance.getSeries();
            Map mapSeries = new HashMap();
            mapSeries.put(series, colInstance);
            Dataset ds = makeIAN(series.getStudy(), mapSeries);
            getPrivateInstance(instance, DELETED, null);
            instance.remove();
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series);
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series.getStudy());
            return ds;
        } catch (CreateException e) {
            throw new RemoteException("Can't move instance to trash, pk: " + instance_pk, e);
        } catch (EJBException e) {
        	throw new RemoteException("Can't move instance to trash, pk: " + instance_pk, e);
        } catch (FinderException e) {
        	throw new RemoteException("Can't move instance to trash, pk: " + instance_pk, e);
        } catch (RemoveException e) {
        	throw new RemoteException("Can't move instance to trash, pk: " + instance_pk, e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset moveSeriesToTrash(long series_pk) throws RemoteException {
        try {
            SeriesLocal series = seriesHome
                    .findByPrimaryKey(new Long(series_pk));
            StudyLocal study = series.getStudy();
            Map mapSeries = new HashMap();
            mapSeries.put(series, series.getInstances());
            Dataset ds = makeIAN(series.getStudy(), mapSeries);
            getPrivateSeries(series, DELETED, null, true);
            series.remove();
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
            return ds;
        } catch (CreateException e) {
        	throw new RemoteException("Can't move series to trash, pk: " + series_pk, e);
        } catch (EJBException e) {
        	throw new RemoteException("Can't move series to trash, pk: " + series_pk, e);
        } catch (FinderException e) {
        	throw new RemoteException("Can't move series to trash, pk: " + series_pk, e);
        } catch (RemoveException e) {
        	throw new RemoteException("Can't move series to trash, pk: " + series_pk, e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Collection moveSeriesToTrash(String[] uids) {
        Collection result = new ArrayList();
        Map mapStudies = new HashMap();
        try {
            for (int i = 0; i < uids.length; i++) {
                SeriesLocal series = seriesHome.findBySeriesIuid(uids[i]);
                StudyLocal study = series.getStudy();
                Map mapSeries = (Map) mapStudies.get(study);
                if (mapSeries == null) {
                    mapStudies.put(study, mapSeries = new HashMap());
                }
                mapSeries.put(series, series.getInstances());
            }
            for (Iterator studyIter = mapStudies.entrySet().iterator();
                    studyIter.hasNext();) {
                Map.Entry studyEntry = (Entry) studyIter.next();
                StudyLocal study = (StudyLocal) studyEntry.getKey();
                Map mapSeries = (Map) studyEntry.getValue();
                result.add(makeIAN(study, mapSeries));
                for (Iterator seriesIter = mapSeries.keySet().iterator();
                        seriesIter.hasNext();) {
                    SeriesLocal series = (SeriesLocal) seriesIter.next();
                    getPrivateSeries(series, DELETED, null, true);
                    series.remove();
                }
                UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
            }
            return result;
        } catch (CreateException e) {
            throw new EJBException(e);
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
       }
    }

    /**
     * @ejb.interface-method
     */
    public Collection moveSeriesOfPPSToTrash(String ppsIUID,
            boolean removeEmptyParents) throws RemoteException {
        Collection result = new ArrayList(); // FIXME: NOT IN USE
        try {
            Object[] ppsSeries = seriesHome.findByPpsIuid(ppsIUID).toArray();
            if (ppsSeries.length > 0) {
                SeriesLocal series = null;
                StudyLocal study = ((SeriesLocal) ppsSeries[0]).getStudy();
                for (int i = 0; i < ppsSeries.length; i++) {
                    series = (SeriesLocal) ppsSeries[i];
                    getPrivateSeries(series, DELETED, null, true);
                    series.remove();
                }
                if (removeEmptyParents && study.getSeries().isEmpty()) {
                    study.remove();
                } else {
                    UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
                }
            }
        } catch (FinderException ignore) {
        } catch (Exception e) {
        	throw new RemoteException("Can't move series of PPS to trash, ppsIUID: " + ppsIUID, e);
        }
        return result;
    }

    /**
     * @ejb.interface-method
     */
    public Dataset moveStudyToTrash(String iuid) throws ObjectNotFoundException {
        try {
            return moveStudyToTrash(studyHome.findByStudyIuid(iuid));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset moveStudyToTrash(long study_pk)
            throws ObjectNotFoundException {
        try {
            return moveStudyToTrash(
                    studyHome.findByPrimaryKey(new Long(study_pk)));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
     }

    private Dataset moveStudyToTrash(StudyLocal study)  {
        try {
            Dataset ds = makeIAN(study, null);
            getPrivateStudy(study, DELETED, null, true);
            study.remove();
            return ds;
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (CreateException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset purgeStudy(String suid)  {
        try {
            StudyLocal study = studyHome.findByStudyIuid(suid);
            Dataset ds = makeIAN(study, null);
            SeriesLocal series;
            FileLocal file;
            for (Iterator iter = study.getSeries().iterator(); iter.hasNext();) {
                series = (SeriesLocal) iter.next();
                for (Iterator iter1 = series.getAllFiles().iterator(); iter1.hasNext();) {
                    file = (FileLocal) iter1.next();
                    privFileHome.create(file.getFilePath(), file.getFileTsuid(), file
                            .getFileSize(), file.getFileMd5(), file.getFileStatus(),
                            null, file.getFileSystem());
                }
            }
            study.remove();
            return ds;
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (CreateException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @ejb.interface-method
     */
    public Collection movePatientToTrash(long pat_pk)
            throws ObjectNotFoundException {
        try {
            return movePatientToTrash(patHome.findByPrimaryKey(new Long(pat_pk)));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Collection movePatientToTrash(Dataset patAttrs, 
            PatientMatching matching) throws ObjectNotFoundException {
        try {
            return movePatientToTrash(
                    patHome.selectPatient(patAttrs, matching, false));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    private Collection movePatientToTrash(PatientLocal patient) {
        Collection col = patient.getStudies();
        Collection result = new ArrayList();
        for (Iterator iter = col.iterator(); iter.hasNext();) {
            result.add(makeIAN((StudyLocal) iter.next(), null));
        }
        Dataset ds = patient.getAttributes(true);
        try {
            getPrivatePatient(patient, DELETED, true);
            patient.remove();
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (CreateException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
        if (result.isEmpty())
            result.add(ds);
        return result;
    }

    private PrivateInstanceLocal getPrivateInstance(InstanceLocal instance,
            int type, PrivateSeriesLocal privSeries) throws FinderException,
            CreateException {
        Collection col = privInstHome
                .findBySopIuid(type, instance.getSopIuid());
        PrivateInstanceLocal privInstance;
        if (col.isEmpty()) {
            if (privSeries == null) {
                privSeries = getPrivateSeries(instance.getSeries(), type, null,
                        false);
            }
            privInstance = privInstHome.create(type, instance
                    .getAttributes(true), privSeries);
        } else {
            privInstance = (PrivateInstanceLocal) col.iterator().next();
        }
        Object[] files = instance.getFiles().toArray();
        FileLocal file;
        for (int i = 0; i < files.length; i++) {
            file = (FileLocal) files[i];
            privFileHome.create(file.getFilePath(), file.getFileTsuid(), file
                    .getFileSize(), file.getFileMd5(), file.getFileStatus(),
                    privInstance, file.getFileSystem());
            try {
                file.remove();
            } catch (Exception x) {
                log.warn("Can not remove File record:" + file, x);
            }
        }
        return privInstance;
    }

    private PrivateSeriesLocal getPrivateSeries(SeriesLocal series, int type,
            PrivateStudyLocal privStudy, boolean includeInstances)
            throws FinderException, CreateException {
        Collection col = privSeriesHome.findBySeriesIuid(type, series
                .getSeriesIuid());
        PrivateSeriesLocal privSeries;
        if (col.isEmpty()) {
            if (privStudy == null) {
                privStudy = getPrivateStudy(series.getStudy(), type, null,
                        false);
            }
            privSeries = privSeriesHome.create(type,
                    series.getAttributes(true), privStudy);
        } else {
            privSeries = (PrivateSeriesLocal) col.iterator().next();
        }
        if (includeInstances) {
            for (Iterator iter = series.getInstances().iterator(); iter
                    .hasNext();) {
                getPrivateInstance((InstanceLocal) iter.next(), type,
                        privSeries);// move also all instances
            }
        }
        return privSeries;
    }

    private PrivateStudyLocal getPrivateStudy(StudyLocal study, int type,
            PrivatePatientLocal privPat, boolean includeSeries)
            throws FinderException, CreateException {
        Collection col = privStudyHome.findByStudyIuid(type, study
                .getStudyIuid());
        PrivateStudyLocal privStudy;
        if (col.isEmpty()) {
            if (privPat == null) {
                privPat = getPrivatePatient(study.getPatient(), type, false);
            }
            privStudy = privStudyHome.create(type, study.getAttributes(true),
                    privPat);
        } else {
            privStudy = (PrivateStudyLocal) col.iterator().next();
        }
        if (includeSeries) {
            for (Iterator iter = study.getSeries().iterator(); iter.hasNext();) {
                getPrivateSeries((SeriesLocal) iter.next(), type, privStudy,
                        true);// move also all instances
            }
        }
        return privStudy;
    }

    private PrivatePatientLocal getPrivatePatient(PatientLocal patient,
            int type, boolean includeStudies) throws FinderException,
            CreateException {
        Collection col = privPatHome.findByPatientIdWithIssuer(type, patient
                .getPatientId(), patient.getIssuerOfPatientId());
        PrivatePatientLocal privPat;
        if (col.isEmpty()) {
            Dataset patAttrs = getAttrsWithUpdatedOtherPatientIDs(patient);
            privPat = privPatHome.create(type, patAttrs);
        } else {
            privPat = (PrivatePatientLocal) col.iterator().next();
        }
        if (includeStudies) {
            for (Iterator iter = patient.getStudies().iterator(); iter
                    .hasNext();) {
                getPrivateStudy((StudyLocal) iter.next(), type, privPat, true);// move
                                                                               // also
                                                                               // all
                                                                               // instances
            }
        }
        return privPat;
    }

    /**
     * Update Other Patient IDs in attributes for patient
     *
     * @param pat Patient whose other patient IDs are to be updated
     */
    private Dataset getAttrsWithUpdatedOtherPatientIDs(PatientLocal pat) {
        Dataset attrs = pat.getAttributes(false);
        // Remove any existing information in Other PIDS sequence and re-populate
        attrs.remove(Tags.OtherPatientIDSeq);
        Collection otherPIDs = pat.getOtherPatientIds();
        if (otherPIDs.size() > 0) {
            DcmElement newOpidSeq = attrs.putSQ(Tags.OtherPatientIDSeq);
            for ( Iterator<?> iter = otherPIDs.iterator() ; iter.hasNext() ; ) { 
                OtherPatientIDLocal opid = (OtherPatientIDLocal) iter.next(); 
                Dataset opidItem = newOpidSeq.addNewItem(); 
                opidItem.putLO(Tags.PatientID, opid.getPatientId()); 
                opidItem.putLO(Tags.IssuerOfPatientID, opid.getIssuerOfPatientId()); 
            }
        }
        return attrs;
    }

    private Dataset makeIAN(StudyLocal study, Map mapSeries) {
        log.debug("makeIAN: studyIUID:" + study.getStudyIuid());
        PatientLocal pat = study.getPatient();
        Dataset ds = dof.newDataset();
        ds.putUI(Tags.StudyInstanceUID, study.getStudyIuid());
//      Don't think attribute PrivateTags.StudyPk is actually used [gz]
//        ds.putOB(PrivateTags.StudyPk, Convert
//                .toBytes(study.getPk().longValue()));
        ds.putSH(Tags.AccessionNumber, study.getAccessionNumber());
        ds.putLO(Tags.PatientID, pat.getPatientId());
        ds.putLO(Tags.IssuerOfPatientID, pat
                .getIssuerOfPatientId());
        ds.putPN(Tags.PatientName, pat.getPatientName());
        DcmElement refPPSSeq = ds.putSQ(Tags.RefPPSSeq);
        HashSet mppsuids = new HashSet();
        DcmElement refSeriesSeq = ds.putSQ(Tags.RefSeriesSeq);

        Iterator iter = (mapSeries == null) ? study.getSeries().iterator()
                : mapSeries.keySet().iterator();
        while (iter.hasNext()) {
            SeriesLocal sl = (SeriesLocal) iter.next();
            MPPSLocal mpps = sl.getMpps();
            if (mpps != null) {
                String mppsuid = mpps.getSopIuid();
                if (mppsuids.add(mppsuid)) {
                    Dataset refmpps = refPPSSeq.addNewItem();
                    refmpps.putUI(Tags.RefSOPClassUID, 
                            UIDs.ModalityPerformedProcedureStep);
                    refmpps.putUI(Tags.RefSOPInstanceUID, mppsuid);
                    refmpps.putSQ(Tags.PerformedWorkitemCodeSeq);
                }
            }
            Dataset dsSer = refSeriesSeq.addNewItem();
            dsSer.putUI(Tags.SeriesInstanceUID, sl.getSeriesIuid());
            Collection instances = (mapSeries == null) ? sl.getInstances()
                    : (Collection) mapSeries.get(sl);
            Iterator iter2 = instances.iterator();
            DcmElement refSopSeq = null;
            if (iter2.hasNext())
                refSopSeq = dsSer.putSQ(Tags.RefSOPSeq);
            while (iter2.hasNext()) {
                InstanceLocal il = (InstanceLocal) iter2.next();
                Dataset dsInst = refSopSeq.addNewItem();
                dsInst.putAE(Tags.RetrieveAET, il.getRetrieveAETs());
                dsInst.putCS(Tags.InstanceAvailability, "UNAVAILABLE");
                dsInst.putUI(Tags.RefSOPClassUID, il.getSopCuid());
                dsInst.putUI(Tags.RefSOPInstanceUID, il.getSopIuid());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("return IAN:");
            log.debug(ds);
        }
        return ds;
    }
}