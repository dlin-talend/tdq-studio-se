// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.record.linkage.ui.action;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.actions.ActionGroup;
import org.talend.dataquality.record.linkage.ui.composite.tableviewer.AbstractMatchAnalysisTableViewer;


/**
 * created by zshen on Aug 2, 2013
 * Detailled comment
 *
 */
public class MatchRuleActionGroup extends ActionGroup {

    private AbstractMatchAnalysisTableViewer tv;

    public MatchRuleActionGroup(AbstractMatchAnalysisTableViewer tv) {
        this.tv = tv;
    }

    @Override
    public void fillContextMenu(IMenuManager mgr) {
        MenuManager menuManager = (MenuManager) mgr;
        menuManager.add(new RemoveMatchKeyDefinitionAction(tv));
        menuManager.add(new RefreshTableViewAction(tv));

        Table table = tv.getTable();
        Menu menu = menuManager.createContextMenu(table);
        table.setMenu(menu);
    }
}
