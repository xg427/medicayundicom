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

package org.dcm4chee.web.war.folder;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.exceptions.SelectionException;
import org.dcm4chee.web.common.model.MultiResourceModel;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.dao.common.DicomEditLocal;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.common.SimpleEditDicomObjectPanel;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;
import org.dcm4chee.web.war.folder.delegate.ContentEditDelegate;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 16700 $ $Date: 2012-03-16 23:47:02 +0800 (周五, 16 3月 2012) $
 * @since Jan 11, 2010
 */
public class MoveEntitiesPage extends SecureSessionCheckPage {
    
    private static final ResourceReference BaseCSS = new CompressedResourceReference(BaseWicketPage.class, "base-style.css");
    private final static ResourceReference CSS = new CompressedResourceReference(MoveEntitiesPage.class, "move-style.css");
    
    private static Logger log = LoggerFactory.getLogger(MoveEntitiesPage.class);

    public static final String MSG_ERR_SELECTION_MOVE_SOURCE_LEVEL = "Selection for move entities wrong! Source must be one level beneath destination !";
    public static final String MSGID_ERR_SELECTION_MOVE_SOURCE_LEVEL = "move.message.error.moveSelectionSrcLevel";
    public static final String MSG_ERR_SELECTION_MOVE_DESTINATION = "Selection for move entities wrong! Only one destination is allowed!";
    public static final String MSGID_ERR_SELECTION_MOVE_DESTINATION = "move.message.error.moveSelectionDest";
    public static final String MSG_ERR_SELECTION_MOVE_NO_SELECTION = "Nothing selected for move entities!";
    public static final String MSGID_ERR_SELECTION_MOVE_NO_SELECTION = "move.message.error.moveNoSelection";
    public static final String MSG_ERR_SELECTION_MOVE_NO_SOURCE = "Selection for move entities wrong! No source entities selected!";
    public static final String MSGID_ERR_SELECTION_MOVE_NO_SOURCE = "move.message.error.moveNoSource";
    public static final String MSG_ERR_SELECTION_MOVE_PPS = "Selection for move entities wrong! PPS entities are not allowed!";
    public static final String MSGID_ERR_SELECTION_MOVE_PPS = "move.message.error.movePPS";
    public static final String MSG_ERR_SELECTION_MOVENOT_ONLINE = "Selection for move entities must have ONLINE availability!";
    public static final String MSGID_ERR_SELECTION_MOVE_NOT_ONLINE = "move.message.error.moveNotOnline";
    public static final String MSGID_ERR_SELECTION_MOVE_NOT_ALLOWED = "folder.message.moveNotAllowed";
    public static final String MSGID_ERR_SELECTION_MOVE_SAME_PARENT = "move.message.error.moveSameParent";
    
    private static final int MISSING_NOTHING = 0;
    private static final int MISSING_STUDY = 1;
    private static final int MISSING_SERIES = 2;

    private SelectedEntities selected;
    private List<PatientModel> allPatients;
    
    private int missingState = MISSING_NOTHING;
    private InfoPanel infoPanel;
    private StudyModel studyModel;
    private SimpleEditDicomObjectPanel newStudyPanel;
    private SeriesModel seriesModel;
    private SimpleEditDicomObjectPanel newSeriesPanel;
    
    private AbstractDicomModel destinationModel;
    private HashSet<AbstractDicomModel> modifiedModels = new HashSet<AbstractDicomModel>();
    private HashSet<AbstractDicomModel> emptySourceParents = new HashSet<AbstractDicomModel>();
    
    private String infoMsgId;
    private IModel<String> selectedInfoModel;
    
    private ModalWindow window;
    
    StudyListLocal dao = (StudyListLocal) JNDIUtils.lookup(StudyListLocal.JNDI_NAME);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MoveEntitiesPage(ModalWindow window, SelectedEntities selectedEntities, List<PatientModel> all) {
        super();
        
        if (MoveEntitiesPage.BaseCSS != null)
            add(CSSPackageResource.getHeaderContribution(MoveEntitiesPage.BaseCSS));
        if (MoveEntitiesPage.CSS != null)
            add(CSSPackageResource.getHeaderContribution(MoveEntitiesPage.CSS));

        this.window = window;
        this.selected = selectedEntities;
        allPatients = all;
        this.add(infoPanel = new InfoPanel());
        infoMsgId = checkSelection(selected);
        if (infoMsgId == null) {
            infoPanel.setSrcInfo(selected);
            StringResourceModel titleModel = null;
            if (selected.hasPatients()) {
                titleModel = new StringResourceModel("move.pageTitle_toPatient",this, new Model(selected.getPatients().iterator().next()));
            } else if (selected.hasStudies()) {
                titleModel = new StringResourceModel("move.pageTitle_toStudy",this, new Model(selected.getStudies().iterator().next()));
            } else if (selected.hasSeries()) {
                titleModel = new StringResourceModel("move.pageTitle_toSeries",this, new Model(selected.getSeries().iterator().next()));
            }
            infoPanel.get("infoTitle").setDefaultModel(titleModel);
        }
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        prepareForMove();
    }
    
    public String checkSelection(SelectedEntities selected) {
        if (selected.getPpss().size() > 0) {
            return MSGID_ERR_SELECTION_MOVE_PPS;
        }
        int pats = selected.getPatients().size();
        if (pats > 1) {
            return MSGID_ERR_SELECTION_MOVE_DESTINATION;
        } 
        if (!checkStudyPermissionOfSelection())
            return MSGID_ERR_SELECTION_MOVE_NOT_ALLOWED;
        if( pats == 1) {
            PatientModel patModel = selected.getPatients().iterator().next();
            if (selected.getStudies().size() < 1) {
                if ( selected.hasSeries() && selected.hasInstances()) {
                    return MSGID_ERR_SELECTION_MOVE_SOURCE_LEVEL;
                } else if (selected.hasSeries()) {
                    for ( SeriesModel m : selected.getSeries()) {
                        if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                            return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                        }
                    }
                    SeriesModel sm = selected.getSeries().iterator().next();
                    needNewStudy(combine(null, sm.getDataset(), sm.getPPS().getStudy().getDataset()), patModel);
                } else if (selected.hasInstances()) {
                    for ( InstanceModel m : selected.getInstances()) {
                        if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                            return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                        }
                    }
                    InstanceModel im = selected.getInstances().iterator().next();
                    needNewStudy(combine(im.getDataset(), im.getSeries().getDataset(),im.getSeries().getPPS().getStudy().getDataset()), patModel);
                    needNewSeries(im, this.studyModel);
                } else {
                    return MSGID_ERR_SELECTION_MOVE_NO_SOURCE;
                }
            } else if (selected.hasSeries() || selected.hasInstances()) {
                return MSGID_ERR_SELECTION_MOVE_SOURCE_LEVEL;
            } else {
                for ( StudyModel m : selected.getStudies()) {
                    if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                        return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                    }
                    if (m.getPatient().equals(patModel)) {
                        return MSGID_ERR_SELECTION_MOVE_SAME_PARENT;
                    }
                }
            }
            destinationModel = patModel;
            return null;
        } 
        // series(inst) -> study
        int nrOfStudies = selected.getStudies().size();
        if ( nrOfStudies > 1) {
            return MSGID_ERR_SELECTION_MOVE_DESTINATION;
        } 
        if( nrOfStudies == 1) {
            if (selected.getSeries().size() < 1) {
                if ( selected.hasInstances()) {
                    for ( InstanceModel m : selected.getInstances()) {
                        if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                            return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                        }
                    }
                    needNewSeries(selected.getInstances().iterator().next(), 
                            selected.getStudies().iterator().next());
                } else {
                    return MSGID_ERR_SELECTION_MOVE_NO_SOURCE;
                }
            } else {
                StudyModel studyModel= selected.getStudies().iterator().next();
                for ( SeriesModel m : selected.getSeries()) {
                    if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                        return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                    }
                    if (m.getPPS().getStudy().equals(studyModel)) {
                        return MSGID_ERR_SELECTION_MOVE_SAME_PARENT;
                    }
                }
            }
            destinationModel = selected.getStudies().iterator().next();
            return null;
        }
        // instances -> series
        int nrOfSeries = selected.getSeries().size();
        if ( nrOfSeries > 1) {
            return MSGID_ERR_SELECTION_MOVE_DESTINATION;
        } 
        if( nrOfSeries == 1) {
            if (selected.getInstances().size() < 1) {
                return MSGID_ERR_SELECTION_MOVE_NO_SOURCE;
            }
            SeriesModel seriesModel= selected.getSeries().iterator().next();
            for ( InstanceModel m : selected.getInstances()) {
                if (Availability.valueOf(m.getAvailability()) != Availability.ONLINE) {
                    return MSGID_ERR_SELECTION_MOVE_NOT_ONLINE;
                }
                if (m.getSeries().equals(seriesModel)) {
                    return MSGID_ERR_SELECTION_MOVE_SAME_PARENT;
                }
            }
            destinationModel = selected.getSeries().iterator().next();
            return null;
        }
        return MSGID_ERR_SELECTION_MOVE_NO_SELECTION;
    }
    
    private boolean checkStudyPermissionOfSelection() {
        StudyPermissionHelper sph = StudyPermissionHelper.get();
        if (!sph.isUseStudyPermissions())
            return true;
        String action = (selected.getPatients().size() > 0) ? StudyPermission.DELETE_ACTION : StudyPermission.APPEND_ACTION;
        if (selected.getStudies().size() > 0) {
            if (!sph.checkPermission(selected.getStudies(), action, true))
                return false;
            action = StudyPermission.DELETE_ACTION;
        }
        if (selected.getSeries().size() > 0) {
            if (!sph.checkPermission(selected.getSeries(), action, true))
                return false;
            action = StudyPermission.DELETE_ACTION;
        }
        if (selected.getInstances().size() > 0) {
            if (!sph.checkPermission(selected.getInstances(), action, true))
                return false;
        }
        return true;
    }
    
    private DicomObject combine(DicomObject instAttrs, DicomObject seriesAttrs, DicomObject studyAttrs) {
        DicomObject attrs = new BasicDicomObject();
        if (instAttrs != null)
            instAttrs.copyTo(attrs);
        if (seriesAttrs != null)
            seriesAttrs.copyTo(attrs);
        if (studyAttrs != null)
            studyAttrs.copyTo(attrs);
        return attrs;
    }
    
    private void needNewStudy(final DicomObject presetDS, final PatientModel pat) {
        missingState = missingState | MISSING_STUDY;
        studyModel = new StudyModel(null, pat, null);
        newStudyPanel = 
            new SimpleEditDicomObjectPanel(
                    "content", 
                    window, 
                    studyModel, 
                    new ResourceModel("move.newStudyForMove.text").wrapOnAssignment(this).getObject(),
                    getStudyEditAttributes(), 
                    false
            ) {
                private static final long serialVersionUID = 1L;
              
                @Override
                protected void onCancel() {
                    doCancel();
                }
               
                @Override
                protected void onClose() {
                    doCancel();
                }

                @Override
                protected void onSubmit() {
                    studyModel.update(getDicomObject());
                    dao.copyStudyPermissions(presetDS.getString(Tag.StudyInstanceUID), studyModel.getStudyInstanceUID());
                    selected.getPatients().clear();
                    selected.getStudies().add(studyModel);
                    missingState = missingState & ~MISSING_STUDY;
                    setResponsePage(MoveEntitiesPage.this);
                }
            };
            presetStudy(presetDS, newStudyPanel);
    }

    private void presetStudy(DicomObject srcAttrs, SimpleEditDicomObjectPanel editor) {
        DicomObject attrs = editor.getDicomObject();
        editor.addPresetChoice(Tag.AccessionNumber, "Accession Number", srcAttrs.getString(Tag.AccessionNumber));
        editor.addPresetChoice(Tag.StudyID, "Study ID", srcAttrs.getString(Tag.StudyID));
        attrs.putString(Tag.StudyDescription, VR.LO, srcAttrs.getString(Tag.SeriesDescription));
        editor.addPresetChoice(Tag.StudyDescription, "Study Description", srcAttrs.getString(Tag.StudyDescription));
        editor.addPresetChoice(Tag.StudyDescription, "Series Description", srcAttrs.getString(Tag.SeriesDescription));
        boolean dateNotPresetted = true;
        if (editor.addPresetChoice(Tag.StudyTime, "Series Date", srcAttrs.getDate(Tag.SeriesDate, Tag.SeriesTime))) {
            attrs.putDate(Tag.StudyDate, VR.DA, srcAttrs.getDate(Tag.SeriesDate));
            attrs.putDate(Tag.StudyTime, VR.TM, srcAttrs.getDate(Tag.SeriesTime));
            dateNotPresetted = false;
        }
        if (editor.addPresetChoice(Tag.StudyTime, "Instance Date", srcAttrs.getDate(Tag.InstanceCreationDate, Tag.InstanceCreationTime)) && dateNotPresetted) {
            attrs.putDate(Tag.StudyDate, VR.DA, srcAttrs.getDate(Tag.InstanceCreationDate));
            attrs.putDate(Tag.StudyTime, VR.TM, srcAttrs.getDate(Tag.InstanceCreationTime));
            dateNotPresetted = false;
        }
        if (editor.addPresetChoice(Tag.StudyTime, "Study Date", srcAttrs.getDate(Tag.StudyDate, Tag.StudyTime)) && dateNotPresetted) {
            attrs.putDate(Tag.StudyDate, VR.DA, srcAttrs.getDate(Tag.StudyDate));
            attrs.putDate(Tag.StudyTime, VR.TM, srcAttrs.getDate(Tag.StudyTime));
            dateNotPresetted = false;
        }
        editor.addPresetChoice(Tag.StudyTime, "Current Date", new Date());
        log.debug("####### presetStudy attrs:"+attrs);
    }

    private void needNewSeries(InstanceModel srcInstance, final StudyModel study) {
        missingState = missingState | MISSING_SERIES;
        seriesModel = new SeriesModel(null, null, null);
        new PPSModel(null, seriesModel, study, null);
        newSeriesPanel = 
            new SimpleEditDicomObjectPanel(
                    "content", 
                    window, 
                    seriesModel, 
                    new ResourceModel("move.newSeriesForMove.text").wrapOnAssignment(this).getObject(),
                    getSeriesEditAttributes(), 
                    false
            ) {
                private static final long serialVersionUID = 1L;
    
                @Override
                protected void onCancel() {
                    doCancel();
                }

                @Override
                protected void onClose() {
                    doCancel();
                }

                @Override
                protected void onSubmit() {
                    seriesModel.update(getDicomObject());
                    selected.getStudies().clear();
                    selected.getSeries().add(seriesModel);
                    missingState = missingState & ~MISSING_SERIES;
                    setResponsePage(MoveEntitiesPage.this);
                }
            };
            DicomObject srcAttrs = combine(srcInstance.getDataset(), srcInstance.getSeries().getDataset(), 
                    srcInstance.getSeries().getPPS().getStudy().getDataset());
            presetSeries(srcAttrs, study.getDataset(), srcInstance.getSeries().getSourceAET(),newSeriesPanel);
    }

    private void presetSeries(DicomObject srcAttrs, DicomObject destStudyAttrs, String sourceAET, SimpleEditDicomObjectPanel editor) {
        DicomObject attrs = editor.getDicomObject();
        attrs.putString(Tag.SeriesDescription, VR.LO, srcAttrs.getString(Tag.SeriesDescription));
        attrs.putString(Tag.SeriesNumber, VR.IS, srcAttrs.getString(Tag.SeriesNumber));
        attrs.putString(Tag.Modality, VR.CS, srcAttrs.getString(Tag.Modality));
        attrs.putString(attrs.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID), VR.AE, sourceAET);

        editor.addPresetChoice(Tag.SeriesDescription, "Source Series Description", srcAttrs.getString(Tag.SeriesDescription));
        editor.addPresetChoice(Tag.SeriesDescription, "Source Study Description", srcAttrs.getString(Tag.StudyDescription));
        editor.addPresetChoice(Tag.SeriesDescription, "Destination Study Description", destStudyAttrs.getString(Tag.StudyDescription));
        boolean dateNotPresetted = true;
        if (editor.addPresetChoice(Tag.SeriesTime, "Destination Study Date", destStudyAttrs.getDate(Tag.StudyDate, Tag.StudyTime)) && dateNotPresetted) {
            attrs.putDate(Tag.SeriesDate, VR.DA, destStudyAttrs.getDate(Tag.StudyDate));
            attrs.putDate(Tag.SeriesTime, VR.TM, destStudyAttrs.getDate(Tag.StudyTime));
            dateNotPresetted = false;
        }
        if (editor.addPresetChoice(Tag.SeriesTime, "Source Series Date", srcAttrs.getDate(Tag.SeriesDate, Tag.SeriesTime)) && dateNotPresetted) {
            attrs.putDate(Tag.StudyDate, VR.DA, srcAttrs.getDate(Tag.SeriesDate));
            attrs.putDate(Tag.StudyTime, VR.TM, srcAttrs.getDate(Tag.SeriesTime));
            dateNotPresetted = false;
        }
        if (editor.addPresetChoice(Tag.SeriesTime, "Source Instance Date", srcAttrs.getDate(Tag.InstanceCreationDate, Tag.InstanceCreationTime)) && dateNotPresetted) {
            attrs.putDate(Tag.SeriesDate, VR.DA, srcAttrs.getDate(Tag.InstanceCreationDate));
            attrs.putDate(Tag.SeriesTime, VR.TM, srcAttrs.getDate(Tag.InstanceCreationTime));
            dateNotPresetted = false;
        }
        editor.addPresetChoice(Tag.SeriesTime, "Current Date", new Date());
        if (attrs.containsValue(Tag.RequestAttributesSequence))
            log.debug("Remove RequestAttributesSequence!");
        attrs.remove(Tag.RequestAttributesSequence);
        log.debug("####### presetSeries attrs:"+attrs);

    }
    
    private void doCancel() {
        DicomEditLocal dao = (DicomEditLocal) JNDIUtils.lookup(DicomEditLocal.JNDI_NAME);
        if (seriesModel != null && seriesModel.getPk() != -1) {
            dao.removeSeries(seriesModel.getPk());
            seriesModel.getPPS().getSeries().remove(seriesModel);
        }
        if (studyModel != null && studyModel.getPk() != -1) {
            dao.removeStudy(studyModel.getPk());
            studyModel.getPatient().getStudies().remove(studyModel);
        }
    }

    private boolean prepareForMove() {
        if ((missingState & MISSING_STUDY) != 0) {
            this.addOrReplace(this.newStudyPanel);
        } else if ((missingState & MISSING_SERIES) != 0) {
            this.addOrReplace(this.newSeriesPanel);
        } else {
            this.addOrReplace(this.infoPanel);
            return false;
        }
        return true;
    }
    
    private int[][] getStudyEditAttributes() {
        return new int[][]{{Tag.StudyInstanceUID},
                {Tag.StudyID},
                {Tag.StudyDescription},
                {Tag.AccessionNumber},
                {Tag.StudyDate, Tag.StudyTime}};
    }
    private int[][] getSeriesEditAttributes() {
        return new int[][]{{Tag.SeriesInstanceUID},
        {Tag.SeriesNumber},
        {Tag.Modality},
        {Tag.SeriesDate, Tag.SeriesTime},
        {Tag.SeriesDescription},
        {Tag.BodyPartExamined},{Tag.Laterality}};
    }

    private class InfoPanel extends Panel {

        private static final long serialVersionUID = 1L;
        
        private Label infoLabel;
        private IndicatingAjaxFallbackLink<?> moveBtn;
        private AjaxFallbackLink<?> okBtn, cancelBtn, yesBtn, noBtn;
        
        private IModel<String> infoModel = new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (infoMsgId == null) {
                    return selectedInfoModel.getObject();
                } else {
                    return MoveEntitiesPage.this.getString(infoMsgId);
                }
            }            
        };
        
        public InfoPanel() {
            super("content");
            
            add( new Label("infoTitle", new ResourceModel("move.pageTitle")));
            add( infoLabel = new Label("info", infoModel));
            infoLabel.setOutputMarkupId(true);
            infoLabel.setEscapeModelStrings(false);
            
            moveBtn = new IndicatingAjaxFallbackLink<Object>("moveBtn") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        if (prepareForMove()) {
                            getPage().setOutputMarkupId(true);
                            target.addComponent(getPage());
                            return;
                        }

                        infoMsgId = "move.message.move.running";
                        okBtn.setVisible(false);
                        cancelBtn.setVisible(false);
                        
                        try {
                            int nrOfMovedInstances = ContentEditDelegate.getInstance().moveEntities(selected);
                            if (nrOfMovedInstances > 0) {
                                updateModels();
                                if (emptySourceParents.size() == 0) {
                                    infoMsgId = "move.message.moveDone";
                                    okBtn.setVisible(true);
                                } else {
                                    infoMsgId = "move.message.removeEmpty";
                                    yesBtn.setVisible(true);
                                    noBtn.setVisible(true);
                                }
                            } else if (nrOfMovedInstances == 0) {
                                infoMsgId = "move.message.moveNothing";
                                okBtn.setVisible(true);
                            } else {
                                infoMsgId = "move.message.moveFailed";
                                cancelBtn.setVisible(true);
                            }
                            moveBtn.setVisible(false);
                        } catch (SelectionException x) {
                            log.warn(x.getMessage());
                            infoMsgId = x.getMsgId();
                        }
                    } catch (Throwable t) {
                        log.error("Can not move selected entities!", t);
                        infoMsgId = "move.message.moveFailed";
                    }
                    addToTarget(target);
                }
                
                @Override
                public boolean isEnabled() {
                    return infoMsgId == null;
                }

            };
            moveBtn.add(new Image("moveImg",ImageManager.IMAGE_FOLDER_MOVE)
            .add(new ImageSizeBehaviour("vertical-align: middle;")));
            moveBtn.add(new TooltipBehaviour("folder.", "moveBtn"));
            moveBtn.add(new Label("moveText", new ResourceModel("folder.moveBtn.text"))
                .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
            );
            add(moveBtn.setOutputMarkupId(true));

            okBtn = new AjaxFallbackLink<Object>("moveFinishedBtn") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(AjaxRequestTarget target) {
                    window.close(target);
                }
            };
            okBtn.add(new TooltipBehaviour("folder.", "moveFinishedBtn"));
            okBtn.add(new Label("moveFinishedText", new ResourceModel("folder.moveFinishedBtn.text"))
                .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle"))));
            okBtn.setVisible(false).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
            add(okBtn);

            cancelBtn = new AjaxFallbackLink<Object>("cancelBtn") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(AjaxRequestTarget target) {
                    doCancel();
                    window.close(target);
                }
            };
            cancelBtn.add(new Image("cancelImg",ImageManager.IMAGE_COMMON_CANCEL)
            .add(new ImageSizeBehaviour("vertical-align: middle;")));
            cancelBtn.add(new TooltipBehaviour("folder.", "cancelBtn"));
            add(cancelBtn.add(new Label("cancelLabel", new ResourceModel("cancelBtn"))).setOutputMarkupId(true));
            
            yesBtn = new AjaxFallbackLink<Object>("deleteEmptyBtn") {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(AjaxRequestTarget target) {
                    deleteEmpty();
                    window.close(target);
                }
            };
            yesBtn.add(new Label("deleteEmptyBtnText", new ResourceModel("yesBtn"))
                .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle"))));
            yesBtn.setVisible(false).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
            add(yesBtn);
            
            noBtn = new AjaxFallbackLink<Object>("dontDeleteEmptyBtn") {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(AjaxRequestTarget target) {
                    window.close(target);
                }
            };
            noBtn.add(new Label("dontDeleteEmptyBtnText", new ResourceModel("noBtn"))
                .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle"))));
            noBtn.setVisible(false).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
            add(noBtn);
        }

        private void refreshStudyAndSeries(AbstractDicomModel parent) {
            int level = parent.levelOfModel();
            if ( level == AbstractDicomModel.INSTANCE_LEVEL) {
                parent = parent.getParent();
                level--;
            }
            for ( ; level > 0 ; level-- ) {
                ((AbstractEditableDicomModel)parent).refresh();
                parent = parent.getParent();
            }
        }
        
        public void setSrcInfo(SelectedEntities selected) {
            if (selected.hasInstances()) {
                addModifiedModels(selected.getInstances());
                selectedInfoModel = getSelectedInfoModel(selected.getInstances());
            } else if (selected.hasSeries()) {
                addModifiedModels(selected.getSeries());
                selectedInfoModel = getSelectedInfoModel(selected.getSeries());
            } else if (selected.hasStudies()) {
                addModifiedModels(selected.getStudies());
                selectedInfoModel = getSelectedInfoModel(selected.getStudies());
            }
        }
        
        private void addModifiedModels(Set<? extends AbstractDicomModel> models) {
            for (AbstractDicomModel m : models) {
                modifiedModels.add(m.getParent());
            }
        }

        private MultiResourceModel getSelectedInfoModel(Set<? extends AbstractDicomModel> selected) {
            MultiResourceModel model = new MultiResourceModel();
            int level = selected.iterator().next().levelOfModel();
            String levelString = AbstractDicomModel.LEVEL_STRINGS[level];
            String resourceKey = "move.selectedToMove_"+levelString.toLowerCase()+".text";
            String key;
            HashMap<String, IModel<AbstractDicomModel>> modelMap = new HashMap<String,IModel<AbstractDicomModel>>();
            HashMap<String, Integer[]> paramMap = new HashMap<String,Integer[]>();
            AbstractDicomModel mStudy;
            Integer[] params;
            for (AbstractDicomModel m : selected) {
                mStudy = m;
                while (mStudy.levelOfModel() > AbstractDicomModel.STUDY_LEVEL) {
                    mStudy = mStudy.getParent();
                }
                key = String.valueOf(mStudy.getParent().getPk()) + 
                    (level == AbstractDicomModel.STUDY_LEVEL ? "" : "_"+mStudy.getPk());
                params = paramMap.get(key);
                if (params == null) {
                    params = new Integer[]{1};
                    paramMap.put(key, params);
                    modelMap.put(key, new Model<AbstractDicomModel>(m));
                } else {
                    params[0]++;
                }
            }
            for (String k : paramMap.keySet()) {
                model.addModel(new StringResourceModel(resourceKey, MoveEntitiesPage.this, modelMap.get(k), paramMap.get(k), "selected?"));
            }
            return model;
        }

        private void addToTarget(AjaxRequestTarget target) {
            target.addComponent(infoLabel);
            target.addComponent(okBtn);
            target.addComponent(moveBtn);
            target.addComponent(cancelBtn);
            target.addComponent(yesBtn);
            target.addComponent(noBtn);
        }

        private void updateModels() {
            selected.clear();
            SelectedEntities.deselectAll(allPatients);
            if (studyModel == null && seriesModel == null) {
                refreshStudyAndSeries(destinationModel);
                destinationModel.expand();
            } else {
                if (studyModel != null) {
                    studyModel.refresh();
                    studyModel.expand();
                    if (seriesModel != null) {//new study && new Series -> only one pps and one series
                        studyModel.getPPSs().iterator().next().getSeries().iterator().next().expand();
                    }
                } else if (seriesModel != null) {
                    seriesModel.expand();
                }
            }
            emptySourceParents.clear();
            for (AbstractDicomModel m : modifiedModels) {
                if (m.levelOfModel() == AbstractDicomModel.PPS_LEVEL) {
                    m.getParent().expand();
                    if (dao.countSeriesOfStudy(m.getParent().getPk()) == 0) {
                        emptySourceParents.add(m.getParent());
                    }
                } else {
                    m.expand();
                    if ( (m.levelOfModel() == AbstractDicomModel.PATIENT_LEVEL && 
                            dao.countStudiesOfPatient(m.getPk(), null) == 0) ||
                         (m.levelOfModel() == AbstractDicomModel.STUDY_LEVEL && 
                            dao.countSeriesOfStudy(m.getPk()) == 0) ||
                         (m.levelOfModel() == AbstractDicomModel.SERIES_LEVEL && 
                            dao.countInstancesOfSeries(m.getPk()) == 0) ) {
                        emptySourceParents.add(m);
                    }
                }
                refreshStudyAndSeries(m);
            }
        }
        @SuppressWarnings({ "unchecked" })
        private void deleteEmpty() {
            log.info("Delete empty entries:"+emptySourceParents);
            AbstractDicomModel toDel;
            HashSet<AbstractDicomModel>[] modelsToDel = new HashSet[4];
            String[] moveCmds = {"movePatientsToTrash","moveStudiesToTrash",
                    null,"moveSeriesToTrash"};
            int level;
            for (AbstractDicomModel m : emptySourceParents) {
                toDel = m;
                log.debug("Model to delete:{}", toDel);
                if (toDel.levelOfModel() > AbstractDicomModel.PATIENT_LEVEL) {
                    m = m.getParent();
                    if(m.levelOfModel() == AbstractDicomModel.PPS_LEVEL)
                        m = m.getParent();
                    if(m.levelOfModel() == AbstractDicomModel.STUDY_LEVEL) {
                        if (dao.countSeriesOfStudy(m.getPk()) == 1) {
                            log.debug("set empty StudyModel to delete:{}", m);
                            toDel = m;
                            m = m.getParent();
                        }
                    }
                    if(m.levelOfModel() == AbstractDicomModel.PATIENT_LEVEL) {
                        if (dao.countStudiesOfPatient(m.getPk(), null) == 1) {
                            log.debug("set empty PatientModel to delete:{}", m);
                            toDel = m;
                        }
                    }
                }
                level = toDel.levelOfModel();
                if (modelsToDel[level] == null) {
                    modelsToDel[level] = new HashSet<AbstractDicomModel>();
                }
                log.debug("add Model to delete for level({}):{}", toDel, level);
                modelsToDel[level].add(toDel);
            }
            for (int i = 0 ; i < modelsToDel.length ; i++) {
                log.debug("Delete level {} :", i, modelsToDel[i]);
                if (modelsToDel[i] != null) {
                    try {
                        ContentEditDelegate.getInstance().moveToTrash(moveCmds[i], toPks(modelsToDel[i]));
                        if (i==0) {
                            allPatients.removeAll(modelsToDel[i]);
                        } else {
                            for (AbstractDicomModel m : (HashSet<AbstractDicomModel>)modelsToDel[i]) {
                                if (m.levelOfModel() == AbstractDicomModel.SERIES_LEVEL)
                                    m.getParent().getParent().expand();
                                else
                                    m.getParent().expand();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Ignored: Move empty entries to trash failed! cmd:"+moveCmds[i], e);
                    }
                }
            }
        }

        private long[] toPks(HashSet<AbstractDicomModel> set) {
            long[] la = new long[set.size()];
            int i = 0;
            for (AbstractDicomModel m : set)
                la[i++] = m.getPk();
            return la;
        }
    }
}
