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

package org.dcm4chex.archive.ejb.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public abstract class BaseCmd {
    protected static final Logger log = Logger.getLogger(BaseCmd.class);
    protected DataSource ds;
    protected Connection con;
    protected Statement stmt;
    protected int prevLevel = 0;
    protected String sql = null;
    protected int transactionIsolationLevel;
    
    protected int updateDatabaseMaxRetries = 20;
    protected long updateDatabaseRetryInterval = 500L; // ms
    
    private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

    protected BaseCmd(String dsJndiName, int transactionIsolationLevel, String sql)
			throws SQLException {
    	this.sql = sql;
    	this.transactionIsolationLevel = transactionIsolationLevel;
    	
		if (sql != null)
	        log.debug("SQL: " + sql);

 	   	 try {
	         Context jndiCtx = new InitialContext();
	         try {
	             ds = (DataSource) jndiCtx.lookup(dsJndiName);
	         } finally {
	             try {
	                 jndiCtx.close();
	             } catch (NamingException ignore) {
	             }
	         }
	     } catch (NamingException ne) {
	         throw new RuntimeException(
	                 "Failed to access Data Source: " + dsJndiName, ne);
	     }

        open();
    }
    
    public void open() throws SQLException 
    {	     
    	Exception lastException = null;
        for(int i = 0; i < updateDatabaseMaxRetries; i++)
        {
	    	try {
	            con = ds.getConnection();
	            prevLevel = con.getTransactionIsolation();
	            if (transactionIsolationLevel > 0)
	                con.setTransactionIsolation(transactionIsolationLevel);
				if (sql != null) {
		            stmt = con.prepareStatement(sql, resultSetType, resultSetConcurrency);
				} else {
					stmt = con.createStatement( resultSetType, resultSetConcurrency);
				} 
				if(i > 0)
					log.info("open connection successfully after retries: " + (i+1));
				
				return;
	        } catch (Exception e) {
	        	if(lastException == null || !lastException.getMessage().equals(e.getMessage()))
	        		log.warn("failed to open connection - retry: "+(i+1)+" of "+updateDatabaseMaxRetries, e);
	        	else
	        		log.warn("failed to open connection: got the same exception as above - retry: "+(i+1)+" of "+updateDatabaseMaxRetries);	        	
	        	lastException = e;
				
	            close();

	            try {
					Thread.sleep(updateDatabaseRetryInterval); 
				} catch (InterruptedException e1) { log.warn(e1);} 
	        }
        }
       	throw new SQLException("give up openning connection after all retries");        
    }
	
    public void close() {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignore) {}
            stmt = null;
        }
        if (con != null) {
            try {
                con.setTransactionIsolation(prevLevel);
            } catch (SQLException ignore) {}
            try {
                con.close();
            } catch (SQLException ignore) {}
            con = null;
        }
    }
    
    public final int getUpdateDatabaseMaxRetries() {
        return updateDatabaseMaxRetries;
    }

    public final void setUpdateDatabaseMaxRetries(int updateDatabaseMaxRetries) {
        this.updateDatabaseMaxRetries = updateDatabaseMaxRetries;
    }

    public final long getUpdateDatabaseRetryInterval() {
        return updateDatabaseRetryInterval;
    }

    public final void setUpdateDatabaseRetryInterval(long interval) {
        this.updateDatabaseRetryInterval = interval;
    }
    /**
     * @param resultSetConcurrency The resultSetConcurrency to set.
     */
    public void setResultSetConcurrency(int resultSetConcurrency) {
        this.resultSetConcurrency = resultSetConcurrency;
    }
    /**
     * @param resultSetType The resultSetType to set.
     */
    public void setResultSetType(int resultSetType) {
        this.resultSetType = resultSetType;
    }
}
