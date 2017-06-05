/*
 * Generated by XDoclet - Do not edit!
 */
package org.dcm4chex.archive.ejb.interfaces;

/**
 * Local interface for MPPS.
 * @xdoclet-generated at ${TODAY}
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version 2.19.0-SNAPSHOT
 */
public interface MPPSLocal
   extends javax.ejb.EJBLocalObject
{
   /**
    * Auto-generated Primary Key
    */
   public java.lang.Long getPk(  ) ;

   public java.sql.Timestamp getCreatedTime(  ) ;

   public java.sql.Timestamp getUpdatedTime(  ) ;

   /**
    * SOP Instance UID
    */
   public java.lang.String getSopIuid(  ) ;

   /**
    * PPS Start Datetime
    */
   public java.sql.Timestamp getPpsStartDateTime(  ) ;

   public void setPpsStartDateTime( java.util.Date date ) ;

   /**
    * Station AET
    */
   public java.lang.String getPerformedStationAET(  ) ;

   /**
    * Modality
    */
   public java.lang.String getModality(  ) ;

   /**
    * Modality
    */
   public java.lang.String getAccessionNumber(  ) ;

   public void setPatient( org.dcm4chex.archive.ejb.interfaces.PatientLocal patient ) ;

   public org.dcm4chex.archive.ejb.interfaces.PatientLocal getPatient(  ) ;

   public void setSeries( java.util.Collection series ) ;

   public java.util.Collection getSeries(  ) ;

   public org.dcm4chex.archive.ejb.interfaces.CodeLocal getDrCode(  ) ;

   public boolean isInProgress(  ) ;

   public boolean isCompleted(  ) ;

   public boolean isDiscontinued(  ) ;

   public java.lang.String getPpsStatus(  ) ;

   public java.lang.String asString(  ) ;

   public org.dcm4che.data.Dataset getAttributes(  ) ;

   public void setAttributes( org.dcm4che.data.Dataset ds ) ;

   public boolean isIncorrectWorklistEntrySelected(  ) ;

}