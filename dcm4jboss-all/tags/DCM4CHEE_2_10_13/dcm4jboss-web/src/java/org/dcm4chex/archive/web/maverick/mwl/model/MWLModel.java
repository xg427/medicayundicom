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

package org.dcm4chex.archive.web.maverick.mwl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.BasicFormPagingModel;
import org.dcm4chex.archive.web.maverick.model.PatientModel;
import org.dcm4chex.archive.web.maverick.mwl.MWLConsoleCtrl;

/**
 * @author franz.willer
 *
 * The Model for Media Creation Managment WEB interface.
 */
public class MWLModel extends BasicFormPagingModel {

	/** The session attribute name to store the model in http session. */
	public static final String MWLMODEL_ATTR_NAME = "mwlModel";
	
    /** Errorcode: unsupported action */
	public static final String ERROR_UNSUPPORTED_ACTION = "UNSUPPORTED_ACTION";

	private String[] mppsIDs = null;
	
	/** Holds list of MWLEntries */
	private Map mwlEntries = new HashMap();

	private MWLFilter mwlFilter;
	
	/** True if mwlScpAET is 'local' to allow deletion */
	private boolean isLocal = false;

	/** Comparator to sort list of SPS datasets. */
	private Comparator comparator = new SpsDSComparator();

	// hold patient infos for merge (same patients/stickyPatients model structure as FolderForm) 
    private List patients = new ArrayList();
    private final Set stickyPatients = new HashSet();
    /** Holds reason of merge. */
	private String mergeReason=null;
	/** Used to preselect dominant patient in patient_merge view */
	private PatientModel dominantPat;

	/**
	 * Creates the model.
	 * <p>
	 * Creates the filter instance for this model.
	 */
	private MWLModel(  HttpServletRequest request ) {
		super(request);
		getFilter();
	}
	
	/**
	 * Get the model for an http request.
	 * <p>
	 * Look in the session for an associated model via <code>MWLMODEL_ATTR_NAME</code><br>
	 * If there is no model stored in session (first request) a new model is created and stored in session.
	 * 
	 * @param request A http request.
	 * 
	 * @return The model for given request.
	 */
	public static final MWLModel getModel( HttpServletRequest request ) {
		MWLModel model = (MWLModel) request.getSession().getAttribute(MWLMODEL_ATTR_NAME);
		if (model == null) {
				model = new MWLModel(request);
				request.getSession().setAttribute(MWLMODEL_ATTR_NAME, model);
				model.filterWorkList( true );
		}
		return model;
	}

	public String getModelName() { return "MWL"; }
	
	/**
	 * Returns the Filter of this model.
	 * 
	 * @return MWLFilter instance that hold filter criteria values.
	 */
	public MWLFilter getFilter() {
		if ( mwlFilter == null ) {
			mwlFilter = new MWLFilter();
		}
		return mwlFilter;
	}
	
	/**
	 * Return a list of MWLEntries for display.
	 * 
	 * @return Returns the mwlEntries.
	 */
	public Collection getMwlEntries() {
		return mwlEntries.values();
	}
	
	public MWLEntry getMWLEntry(String spsID) {
		return (MWLEntry) mwlEntries.get(spsID);
	}
	/**
	 * Returns true if the MwlScpAET is 'local'.
	 * <p>
	 * <DL>
	 * <DT>If it is local:</DT>
	 * <DD>  1) Entries can be deleted. (shows a button in view)</DD>
	 * <DD>  2) The query for the working list is done directly without a CFIND.</DD>
	 * </DL>
	 * @return Returns the isLocal.
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * Returns > 0 if MWL console is in 'link' mode.
	 * <p>
	 * This mode is set if mwl_console.m is called with action=link.
	 * <p>
	 * The value is euivalent to the number of selected MPPS (size of mppsIDs).
	 * 
	 * @return Returns the linkMode.
	 */
	public int getLinkMode() {
		return mppsIDs == null ? 0 : mppsIDs.length;
	}
	/**
	 * @return Returns the mppsIDs.
	 */
	public String[] getMppsIDs() {
		return mppsIDs;
	}
	/**
	 * @param mppsIDs The mppsIDs to set.
	 */
	public void setMppsIDs(String[] mppsIDs) {
		this.mppsIDs = mppsIDs;
	}
	
	public void setPatMergeAttributes(Map map) {
		patients.clear();
		stickyPatients.clear();
		if ( map != null ) {
			dominantPat = new PatientModel( (Dataset) map.get("dominant") );
			stickyPatients.add(new Long(dominantPat.getPk()));
			patients.add(dominantPat);
			Dataset[] dsa = (Dataset[]) map.get("priorPats");
			PatientModel pat;
			for ( int i = 0 ; i < dsa.length ; i++ ) {
				pat = new PatientModel( dsa[i]);
				stickyPatients.add(new Long(pat.getPk()));
				patients.add(pat);
			}
			mergeReason="Linking MWL to MPPS requires patient merge!";
		} else {
			dominantPat = null;
			mergeReason = null;
		}
		
	}
	
    public final List getPatients() {
        return patients;
    }
    public final Set getStickyPatients() {
        return stickyPatients;
    }
    public String getMergeReason() {
    	return mergeReason;
    }
	/**
	 * @return Returns the dominant patient for necessary merge.
	 */
	public PatientModel getMergeDominant() {
		return dominantPat;
	}
	/** returns the name of the view used after merge is done. (have to be configured in maverick.xml!) */
    public String getMergeViewName() {
    	return "mpps_link";
    }
	
	/**
	 * Update the list of MWLEntries for the view.
	 * <p>
	 * The query use the search criteria values from the filter and use offset and limit for paging.
	 * <p>
	 * if <code>newSearch is true</code> will reset paging (set <code>offset</code> to 0!)
	 * @param newSearch
	 */
	public void filterWorkList(boolean newSearch) {
		
		if ( newSearch ) setOffset(0);
		Dataset searchDS = getSearchDS( mwlFilter );
		isLocal = MWLConsoleCtrl.getMwlScuDelegate().isLocal();
		List l = MWLConsoleCtrl.getMwlScuDelegate().findMWLEntries( searchDS );
		Collections.sort( l, comparator );
		int total = l.size();
		int offset = getOffset();
		int limit = getLimit();
		int end;
		if ( offset >= total ) {
			offset = 0;
			end = limit < total ? limit : total;
		} else {
			end = offset + limit;
			if ( end > total ) end = total;
		}
		Dataset ds;
		mwlEntries.clear();
		int countNull = 0;
		MWLEntry entry;
		for ( int i = offset ; i < end ; i++ ){
			ds = (Dataset) l.get( i );
			if ( ds != null ) {
				entry = new MWLEntry( ds );
				mwlEntries.put( entry.getSpsID(), entry );
			} else {
				countNull++;
			}
		}
		setTotal(total - countNull); // the real total (without null entries!)	
	}

	/**
	 * Returns the query Dataset with search criteria values from filter argument.
	 * <p>
	 * The Dataset contains all fields that should be in the result; 
	 * the 'merging' fields will be set to the values from the filter.
	 * 
	 * @param mwlFilter2 The filter with search criteria values.
	 * 
	 * @return The Dataset that can be used for query.
	 */
	private Dataset getSearchDS(MWLFilter filter) {
		Dataset ds = DcmObjectFactory.getInstance().newDataset();
		//requested procedure
		ds.putSH( Tags.RequestedProcedureID );
		ds.putUI( Tags.StudyInstanceUID );
		//imaging service request
		ds.putSH( Tags.AccessionNumber, mwlFilter.getAccessionNumber() );
		ds.putLT( Tags.ImagingServiceRequestComments );
		ds.putPN( Tags.RequestingPhysician );
		ds.putPN( Tags.ReferringPhysicianName );
		ds.putLO( Tags.PlacerOrderNumber );
		ds.putLO( Tags.FillerOrderNumber );
		//Visit Identification
		ds.putLO( Tags.AdmissionID );
		//Patient Identification
		String patientName = mwlFilter.getPatientName();
    	if ( patientName != null && 
       		 patientName.length() > 0 && 
   			 patientName.indexOf('*') == -1 &&
   			 patientName.indexOf('?') == -1) patientName+="*";

		ds.putPN( Tags.PatientName, patientName );
		ds.putLO( Tags.PatientID);
		//Patient demographic
		ds.putDA( Tags.PatientBirthDate );
		ds.putCS( Tags.PatientSex );
		//Sched. procedure step seq
		DcmElement elem = ds.putSQ( Tags.SPSSeq );
		Dataset ds1 = elem.addNewItem();
		ds1.putCS(Tags.SPSStatus);
		ds1.putAE( Tags.ScheduledStationAET, filter.getStationAET() );
		ds1.putSH( Tags.SPSID );
		ds1.putCS( Tags.Modality, filter.getModality() );
		ds1.putPN( Tags.ScheduledPerformingPhysicianName );
		if ( filter.getStartDate() != null || filter.getEndDate() != null ) {
			Date startDate = null, endDate = null;
			if ( filter.startDateAsLong() != null ) 
				startDate = new Date ( filter.startDateAsLong().longValue() );
			if ( filter.endDateAsLong() != null ) 
				endDate = new Date ( filter.endDateAsLong().longValue() );
			ds1.putDA( Tags.SPSStartDate, startDate, endDate );
			ds1.putTM( Tags.SPSStartTime, startDate, endDate );
		} else {
			ds1.putDA( Tags.SPSStartDate );
			ds1.putTM( Tags.SPSStartTime );
		}
		ds1.putSH( Tags.ScheduledStationName );
		//sched. protocol code seq;
		DcmElement spcs = ds1.putSQ( Tags.ScheduledProtocolCodeSeq );
		Dataset dsSpcs = spcs.addNewItem();
		dsSpcs.putSH( Tags.CodeValue );
		dsSpcs.putLO( Tags.CodeMeaning );
		dsSpcs.putSH( Tags.CodingSchemeDesignator );
		// or 
		ds1.putLO( Tags.SPSDescription );
		
		//Req. procedure code seq
		DcmElement rpcs = ds.putSQ( Tags.RequestedProcedureCodeSeq );
		Dataset dsRpcs = rpcs.addNewItem();
		dsRpcs.putSH( Tags.CodeValue );
		dsRpcs.putLO( Tags.CodeMeaning );
		dsRpcs.putSH( Tags.CodingSchemeDesignator );
		// or 
		ds.putLO( Tags.RequestedProcedureDescription );

		return ds;
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
	 */
	public void gotoCurrentPage() {
		filterWorkList( false );
	}
	
	/**
	 * Inner class that compares two datasets for sorting Scheduled Procedure Steps 
	 * according scheduled Procedure step start date.
	 * 
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public class SpsDSComparator implements Comparator {

		private final Date DATE_0 = new Date(0l);
		public SpsDSComparator() {
			
		}

		/**
		 * Compares the scheduled procedure step start date and time of two Dataset objects.
		 * <p>
		 * USe either SPSStartDateAndTime or SPSStartDate and SPSStartTime to get the date.
		 * <p>
		 * Use the '0' Date (new Date(0l)) if the date is not in the Dataset.
		 * <p>
		 * Compares its two arguments for order. Returns a negative integer, zero, or a positive integer 
		 * as the first argument is less than, equal to, or greater than the second.
		 * <p>
		 * Throws an Exception if one of the arguments is null or not a Dataset object.
		 * 
		 * @param arg0 	First argument
		 * @param arg1	Second argument
		 * 
		 * @return <0 if arg0<arg1, 0 if equal and >0 if arg0>arg1
		 */
		public int compare(Object arg0, Object arg1) {
			Dataset ds1 = (Dataset) arg0;
			Dataset ds2 = (Dataset) arg1;
			Date d1 = _getStartDateAsLong( ds1 );
			return d1.compareTo( _getStartDateAsLong( ds2 ) );
		}

		/**
		 * @param ds1 The dataset
		 * 
		 * @return the date of this SPS Dataset.
		 */
		private Date _getStartDateAsLong(Dataset ds) {
			if ( ds == null ) return DATE_0;
			DcmElement e = ds.get( Tags.SPSSeq );
			if ( e == null ) return DATE_0;
			Dataset spsItem = e.getItem();//scheduled procedure step sequence item.
			Date d = spsItem.getDate( Tags.SPSStartDateAndTime );
			if ( d == null ) {
				d = spsItem.getDateTime( Tags.SPSStartDate, Tags.SPSStartTime );
			}
			if ( d == null ) d = DATE_0;
			return d;
		}
	}

	
}
