// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.action.provider;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.ui.action.actions.CreateNewAnalysisAction;

/**
 * @author rli
 * 
 */
public class NewAnalysisActionProvider extends CommonActionProvider {

    public NewAnalysisActionProvider() {
    }

    /**
     * Adds a submenu to the given menu with the name "New Component".
     */
    public void fillContextMenu(IMenuManager menu) {
        Object obj = ((TreeSelection) this.getContext().getSelection()).getFirstElement();
        if (obj instanceof IFolder) {
            IFolder folder = (IFolder) obj;
            try {
                if (DQStructureManager.ANALYSIS_FOLDER_PROPERTY.equals(folder
                        .getPersistentProperty(DQStructureManager.FOLDER_CLASSIFY_KEY))) {
                    CreateNewAnalysisAction createAnalysisAction = new CreateNewAnalysisAction(folder);
                    menu.add(createAnalysisAction);
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

}
