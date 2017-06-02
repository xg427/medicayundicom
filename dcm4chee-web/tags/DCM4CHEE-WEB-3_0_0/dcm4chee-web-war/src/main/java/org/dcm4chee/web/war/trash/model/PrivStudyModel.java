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
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2008
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

package org.dcm4chee.web.war.trash.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.entity.PrivateStudy;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.trash.TrashListLocal;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since May 10, 2010
 */
public class PrivStudyModel extends AbstractDicomModel implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private List<PrivSeriesModel> seriess = new ArrayList<PrivSeriesModel>();

    public PrivStudyModel(PrivateStudy study, PrivPatientModel patientModel) {
        setPk(study.getPk());
        dataset = study.getAttributes();
        setPatient(patientModel);
    }

    public void setPatient(PrivPatientModel m) {
        setParent(m);
    }

    public PrivPatientModel getPatient() {
        return (PrivPatientModel) getParent();
    }

    public String getStudyInstanceUID() {
        return dataset.getString(Tag.StudyInstanceUID, "");
    }

    public Date getDatetime() {
        return toDate(Tag.StudyDate, Tag.StudyTime);
    }

    public void setDatetime(Date datetime) {
        
        dataset.putDate(Tag.StudyDate, VR.DA, datetime);
        dataset.putDate(Tag.StudyTime, VR.TM, datetime);
    }
    
    public String getId() {
        return dataset.getString(Tag.StudyID);
    }

    public String getAccessionNumber() {
        return dataset.getString(Tag.AccessionNumber, "");
    }

    public String getModalities() {
        return toString(dataset.getStrings(Tag.ModalitiesInStudy));
    }

    private String toString(String[] ss) {
        if (ss == null || ss.length == 0) {
            return null;
        }
        if (ss.length == 1) {
            return ss[0];
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ss[0]);
        for (int i = 1; i < ss.length; i++) {
            sb.append('\\').append(ss[i]);
        }
        return sb.toString();
    }

    public String getDescription() {
        return dataset.getString(Tag.StudyDescription);
    }

    public void setDescription(String description) {
        dataset.putString(Tag.StudyDescription, VR.LO, description);
    }

    public Long getNumberOfSeries() {
        return ((TrashListLocal) JNDIUtils.lookup(TrashListLocal.JNDI_NAME)).getNumberOfSeriesOfStudy(this.getPk());
    }

    public Long getNumberOfInstances() {
        return ((TrashListLocal) JNDIUtils.lookup(TrashListLocal.JNDI_NAME)).getNumberOfInstancesOfStudy(this.getPk());
    }

    public String getAvailability() {
        return dataset.getString(Tag.InstanceAvailability);
    }

    public List<PrivSeriesModel> getSeries() {
        return seriess;
    }

    @Override
    public int getRowspan() {
        int rowspan = isDetails() ? 2 : 1;
        for (PrivSeriesModel series : seriess) {
            rowspan += series.getRowspan();
        }
        return rowspan;
    }

    @Override
    public void collapse() {
        seriess.clear();
    }

    @Override
    public boolean isCollapsed() {
        return seriess.isEmpty();
    }

    public void retainSelectedSeries() {
        PrivSeriesModel s;
        for (Iterator<PrivSeriesModel> it = seriess.iterator() ; it.hasNext();) {
            s = it.next();
            s.retainSelectedInstances();
            if (s.isCollapsed() && !s.isSelected()) {
                it.remove();
            }
        }
    }

    @Override
    public void expand() {
        this.seriess.clear();
        TrashListLocal dao = (TrashListLocal)
                JNDIUtils.lookup(TrashListLocal.JNDI_NAME);
        for (PrivateSeries series : dao.findSeriesOfStudy(getPk())) {
            seriess.add(new PrivSeriesModel(series, this));
        }
    }

    @Override
    public int levelOfModel() {
        return STUDY_LEVEL;
    }
   
    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return seriess;
    }
}
