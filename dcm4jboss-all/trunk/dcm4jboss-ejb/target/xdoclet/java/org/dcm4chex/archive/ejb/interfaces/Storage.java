/*
 * Generated by XDoclet - Do not edit!
 */
package org.dcm4chex.archive.ejb.interfaces;

/**
 * Remote interface for Storage.
 * @xdoclet-generated at ${TODAY}
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version 2.19.0-SNAPSHOT
 */
public interface Storage
   extends javax.ejb.EJBObject
{

   public org.dcm4che.data.Dataset store( org.dcm4che.data.Dataset ds,java.lang.String currentCallingAET,long fspk,java.lang.String fileid,long size,byte[] md5,byte[] origMd5,int fileStatus,boolean updateStudyAccessTime,boolean clearExternalRetrieveAET,boolean dontChangeReceivedStatus,org.dcm4chex.archive.common.PatientMatching matching )
      throws org.dcm4che.net.DcmServiceException, org.dcm4chex.archive.exceptions.NonUniquePatientIDException, java.rmi.RemoteException;

   public org.dcm4che.data.Dataset store( org.dcm4che.data.Dataset ds,java.lang.String currentCallingAET,long fspk,java.lang.String fileid,long size,byte[] md5,byte[] origMd5,int fileStatus,boolean updateStudyAccessTime,boolean clearExternalRetrieveAET,boolean dontChangeReceivedStatus,org.dcm4chex.archive.common.PatientMatching matching,boolean canRollback )
      throws org.dcm4che.net.DcmServiceException, org.dcm4chex.archive.exceptions.NonUniquePatientIDException, java.rmi.RemoteException;

   public java.util.Collection getDuplicateFiles( org.dcm4chex.archive.ejb.interfaces.FileDTO dto )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public org.dcm4chex.archive.common.SeriesStored makeSeriesStored( java.lang.String seriuid )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public void commitSeriesStored( org.dcm4chex.archive.common.SeriesStored seriesStored )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public java.util.Collection getPksOfPendingSeries( java.sql.Timestamp updatedBefore )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public java.util.Collection getPksOfPendingSeries( java.util.Map delays )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public org.dcm4chex.archive.common.SeriesStored makeSeriesStored( java.lang.Long seriesPk,java.sql.Timestamp updatedBefore )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public void updateDerivedStudyAndSeriesFields( java.lang.String seriuid )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public void storeFile( java.lang.String iuid,java.lang.String tsuid,java.lang.String dirpath,java.lang.String fileid,int size,byte[] md5,byte[] origMd5,int status )
      throws javax.ejb.CreateException, javax.ejb.FinderException, java.rmi.RemoteException;

   public org.dcm4chex.archive.ejb.interfaces.PatientLocal createPatient( org.dcm4che.data.Dataset ds )
      throws javax.ejb.CreateException, java.rmi.RemoteException;

   public void deletePatient( org.dcm4chex.archive.ejb.interfaces.PatientLocal pat )
      throws javax.ejb.RemoveException, java.rmi.RemoteException;

   public void commit( java.lang.String iuid )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public void commited( org.dcm4che.data.Dataset stgCmtResult )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public int numberOfStudyRelatedInstances( java.lang.String iuid )
      throws java.rmi.RemoteException;

   public boolean studyExists( java.lang.String iuid )
      throws java.rmi.RemoteException;

   public boolean instanceExists( java.lang.String iuid )
      throws java.rmi.RemoteException;

   public void deleteInstances( java.lang.String[] iuids,boolean deleteSeries,boolean deleteStudy )
      throws javax.ejb.FinderException, javax.ejb.RemoveException, java.rmi.RemoteException;

   public void removeFromSeriesPkCache( java.lang.String uid )
      throws java.rmi.RemoteException;

   public org.dcm4che.data.Dataset getPatientByIDWithIssuer( java.lang.String pid,java.lang.String issuer )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

   public java.util.List getSopIuidsForRejectionNote( org.dcm4che.data.Dataset rejNote,java.lang.String srcAet )
      throws javax.ejb.FinderException, java.rmi.RemoteException;

}
