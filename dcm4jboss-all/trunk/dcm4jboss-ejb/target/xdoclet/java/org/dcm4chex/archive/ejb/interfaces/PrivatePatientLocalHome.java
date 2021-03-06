/*
 * Generated by XDoclet - Do not edit!
 */
package org.dcm4chex.archive.ejb.interfaces;

/**
 * Local home interface for PrivatePatient.
 * @xdoclet-generated at ${TODAY}
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version 2.19.0-SNAPSHOT
 */
public interface PrivatePatientLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/PrivatePatientLocal";
   public static final String JNDI_NAME="ejb/PrivatePatient";

   public org.dcm4chex.archive.ejb.interfaces.PrivatePatientLocal create(int type , org.dcm4che.data.Dataset ds)
      throws javax.ejb.CreateException;

   public java.util.Collection findByPrivateType(int privateType)
      throws javax.ejb.FinderException;

   public java.util.Collection findByPatientId(int privateType, java.lang.String pid)
      throws javax.ejb.FinderException;

   public java.util.Collection findByPatientIdWithIssuer(int privateType, java.lang.String pid, java.lang.String issuer)
      throws javax.ejb.FinderException;

   public org.dcm4chex.archive.ejb.interfaces.PrivatePatientLocal findByPrimaryKey(java.lang.Long pk)
      throws javax.ejb.FinderException;

}
