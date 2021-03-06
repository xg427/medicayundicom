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

package org.dcm4chex.archive.hl7;

import java.io.Serializable;
import java.util.List;

import org.dcm4che.hl7.HL7;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Factory;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4chex.archive.common.BaseJmsOrder;
import org.dcm4chex.archive.common.JmsOrderProperties;

public class HL7SendOrder extends BaseJmsOrder implements Serializable {

	private static final long serialVersionUID = 3257003259104147767L;

	private final byte[] hl7msg;

	private final String receiving;

	public HL7SendOrder(byte[] hl7msg, String receiving) {
		if (hl7msg == null)
			throw new NullPointerException();
		if (receiving == null)
			throw new NullPointerException();
		this.hl7msg = hl7msg;
		this.receiving = receiving;
	}
	
    public final byte[] getHL7Message() {
        return hl7msg;
    }

	public final String getReceiving() {
		return receiving;
	}

    public String getOrderDetails() {
        return "receiving=" + receiving;
    }
    
    /**
     * Processes order attributes based on the HL7 message set in the {@code ctor}.
     * @see BaseJmsOrder#processOrderProperties(Object...)
     */
    @Override
    public void processOrderProperties(Object... properties) {
        try {
            List<?> segments = HL7Factory.getInstance().parse(hl7msg).segments();
            for ( int i = 0; i < segments.size(); i++ ) {
                HL7Segment seg = (HL7Segment) segments.get(i);
                if ( seg.id().equals("PID") ) {
                    // the sequence we want from the PID segment looks like "1002^^^THE_ISSUER"
                    String patientID = seg.get(HL7.PIDPatientIDInternalID);
                    String id, issuer = null;
                    if ( patientID.contains("^") ) {
                        id = patientID.substring(0, patientID.indexOf('^'));
                        issuer = patientID.substring(patientID.lastIndexOf('^') + 1, patientID.length());
                    }
                    else {
                        id = patientID;
                    }
                    this.setOrderProperty(JmsOrderProperties.ISSUER_OF_PATIENT_ID, issuer);
                    this.setOrderProperty(JmsOrderProperties.PATIENT_ID, id);
                    break;
                }
            }
            
            if ( properties != null && properties.length == 1 && properties[0] instanceof MSH ) {
                MSH msh = (MSH) properties[0];
                this.setOrderProperty(JmsOrderProperties.MESSAGE_TYPE, msh.messageType);
                this.setOrderProperty(JmsOrderProperties.TRIGGER_EVENT, msh.triggerEvent);
            }
        }
        catch (HL7Exception e) {
            throw new RuntimeException("Failed to parse PID segment of message", e);
        }
    }
}
