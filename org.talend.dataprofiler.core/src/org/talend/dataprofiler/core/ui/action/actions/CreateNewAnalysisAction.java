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
package org.talend.dataprofiler.core.ui.action.actions;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.talend.cwm.management.api.FolderProvider;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.wizard.analysis.CreateNewAnalysisWizard;
import org.talend.dataprofiler.core.ui.wizard.analysis.WizardFactory;
import org.talend.dataquality.analysis.AnalysisType;
import org.talend.dq.analysis.parameters.AnalysisLabelParameter;

/**
 * DOC zqin class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40Z zqin $
 * 
 */
public class CreateNewAnalysisAction extends Action implements ICheatSheetAction {

    public CreateNewAnalysisAction() {
        super(DefaultMessagesImpl.getString("CreateNewAnalysisAction.newAnalysis")); //$NON-NLS-1$
        setImageDescriptor(ImageLib.getImageDescriptor(ImageLib.ACTION_NEW_ANALYSIS));
    }

    public CreateNewAnalysisAction(IFolder folder) {
        this();
        this.folder = folder;
    }

    private IFolder folder;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        FolderProvider currentFolderProvider = new FolderProvider();
        currentFolderProvider.setFolderResource(folder);

        CreateNewAnalysisWizard wizard = WizardFactory.createNewAnalysisWizard();
        wizard.setCurrentFolderProvider(currentFolderProvider);
        wizard.setForcePreviousAndNextButtons(true);
        WizardDialog dialog = new WizardDialog(null, wizard);

        dialog.open();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.cheatsheets.ICheatSheetAction#run(java.lang.String[],
     * org.eclipse.ui.cheatsheets.ICheatSheetManager)
     */
    public void run(String[] params, ICheatSheetManager manager) {
        // MOD mzhao 2009-02-03 open analysis creation wizard according to
        // passed parameter.
        if (params == null || params.length == 0) {
            return;
        }
        Integer analysisCatigory = null;
        if (NumberUtils.isNumber(params[0])) {
            analysisCatigory = NumberUtils.toInt(params[0]);
        }
        AnalysisType analysisType = null;
        Wizard wizard = null;
        if (analysisCatigory != null) {
            switch (analysisCatigory) {
            case AnalysisType.MULTIPLE_COLUMN_VALUE:
                analysisType = AnalysisType.MULTIPLE_COLUMN;
                wizard = WizardFactory.createAnalysisWizard(analysisType);
                break;
            case AnalysisType.CATALOG_VALUE:
                analysisType = AnalysisType.CATALOG;
                wizard = WizardFactory.createAnalysisWizard(analysisType);
                break;
            case AnalysisType.SCHEMA_VALUE:
                analysisType = AnalysisType.SCHEMA;
                wizard = WizardFactory.createAnalysisWizard(analysisType);
                break;
            case AnalysisType.COLUMNS_COMPARISON_VALUE:
                analysisType = AnalysisType.COLUMNS_COMPARISON;
                wizard = WizardFactory.createAnalysisWizard(analysisType);
                break;
            case AnalysisType.COLUMN_CORRELATION_VALUE:
                analysisType = AnalysisType.COLUMN_CORRELATION;
                if (params[1] != null) {
                    if (NumberUtils.isNumber(params[1])) {
                        AnalysisLabelParameter parameter = new AnalysisLabelParameter();
                        parameter.setAnalysisTypeName(analysisType.getLiteral());
                        int correAnaType = NumberUtils.toInt(params[1]);
                        if (correAnaType == 0) {
                            parameter.setCategoryLabel(AnalysisLabelParameter.NUMBERIC_CORRELATION);
                        } else {
                            parameter.setCategoryLabel(AnalysisLabelParameter.DATE_CORRELATION);
                        }
                        wizard = WizardFactory.createAnalysisWizard(analysisType, parameter);
                    }
                }
                break;
            default:
                break;
            }
        }
        if (analysisType == null || wizard == null) {
            return;
        }
        wizard.setForcePreviousAndNextButtons(true);
        WizardDialog dialog = new WizardDialog(null, wizard);
        dialog.setPageSize(500, 340);

        dialog.open();
    }
}
