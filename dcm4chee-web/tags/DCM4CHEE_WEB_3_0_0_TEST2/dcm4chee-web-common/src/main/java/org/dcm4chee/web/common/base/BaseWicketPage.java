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

package org.dcm4chee.web.common.base;

import org.apache.wicket.Application;
import org.apache.wicket.IPageMap;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since July 7, 2009
 */
public class BaseWicketPage extends WebPage {

    private ModuleSelectorPanel selectorPanel;

    private static final ResourceReference CSS = new CompressedResourceReference(BaseWicketPage.class, "base-style.css");

    public BaseWicketPage() {
        super();
        initLayout();
    }

    public BaseWicketPage(IPageMap pageMap) {
        super(pageMap);
        initLayout();
    }
    
    private void initLayout() {
        add( new Label("app_browser_title", new AbstractReadOnlyModel<Object>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return getBrowserTitle();
            }
        } ));
        selectorPanel = new ModuleSelectorPanel("modules");
        if ( getCSS() != null)
            add(CSSPackageResource.getHeaderContribution(getCSS()));
        add(selectorPanel);
    }

    public ModuleSelectorPanel getModuleSelectorPanel() {
        return selectorPanel;
    }

    protected ResourceReference getCSS() {
        return CSS;
    }

    protected String getBrowserTitle() {
        Class<?> clazz = Application.get().getHomePage();
        String s = new ClassStringResourceLoader(clazz).loadStringResource(null, "application.browser_title");
        if (s==null) {
            s = new PackageStringResourceLoader().loadStringResource(clazz, "application.browser_title", 
                    getSession().getLocale(), null);
        }
        return s == null ? "DCM4CHEE" : s;
    }
}
