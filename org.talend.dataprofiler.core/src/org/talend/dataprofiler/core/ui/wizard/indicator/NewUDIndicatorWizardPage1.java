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
package org.talend.dataprofiler.core.ui.wizard.indicator;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.ui.wizard.MetadataWizardPage;
import org.talend.resource.ResourceManager;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class NewUDIndicatorWizardPage1 extends MetadataWizardPage {

    protected static Logger log = Logger.getLogger(NewUDIndicatorWizardPage1.class);

    protected HashMap<String, String> metadata;

    public NewUDIndicatorWizardPage1() {
        metadata = new HashMap<String, String>();
        setPageComplete(false);
    }

    public void createControl(Composite parent) {

        super.createControl(parent);
        pathText.setText(getParameter().getFolderProvider().getFolderURI());

        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                openFolderSelectionDialog(ResourceManager.LIBRARIES_FOLDER_NAME, DQStructureManager.INDICATORS);
            }
        });
    }

    @Override
    protected void createExtendedControl(Composite container) {
    }

    @Override
    protected IFolder getStoredFolder() {
        return ResourceManager.getLibrariesFolder().getFolder(DQStructureManager.INDICATORS);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        this.nameText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                getParameter().setName(nameText.getText());
                setPageComplete(nameText.getText() != null && !"".equals(nameText.getText().trim()));
            }
        });
    }

}
