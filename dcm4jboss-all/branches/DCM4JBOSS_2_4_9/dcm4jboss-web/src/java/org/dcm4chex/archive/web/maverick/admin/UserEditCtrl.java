/* $Id: UserEditCtrl.java 1857 2005-07-14 15:39:24Z javawilli $
 * Copyright (c) 2002,2003 by TIANI MEDGRAPH AG
 *
 * This file is part of dcm4che.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.dcm4chex.archive.web.maverick.admin;


/**
 * @author franz.willer@gwi-ag.com
 */
public class UserEditCtrl extends UserAdminCtrl
{

	protected String perform() throws Exception 
	{
		getModel().selectEditUser( getCtx().getRequest().getParameter("userHash" ) );
		if ( ! model.isAdmin() ) return "error";
		return "success";
	}

}


