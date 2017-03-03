package org.talend.dataprofiler.core.ui.dialog;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataprofiler.core.ui.views.provider.DQRepositoryViewLabelProvider;
import org.talend.dataprofiler.core.ui.views.provider.ResourceViewContentProvider;
import org.talend.resource.EResourceConstant;


public class PatternSelectionForComponentDialog extends ElementTreeSelectionDialog {

    public PatternSelectionForComponentDialog(Shell parent, IBaseLabelProvider labelProvider, ITreeContentProvider contentProvider) {
        super(parent, labelProvider, contentProvider);
    }

    public PatternSelectionForComponentDialog(Shell parent) {
        super(parent,new DQRepositoryViewLabelProvider(), new ResourceViewContentProvider());
        setInput(AnalysisUtils.getSelectDialogInputData(EResourceConstant.PATTERN_REGEX));

        setTitle(DefaultMessagesImpl.getString("PatternSelectionForComponentDialog.title")); //$NON-NLS-1$
        setMessage(DefaultMessagesImpl.getString("PatternSelectionForComponentDialog.rule")); //$NON-NLS-1$

    }

    @Override
    protected Control createDialogArea(final Composite parent) {

        Composite result = (Composite) super.createDialogArea(parent);
        
        this.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                
            }
        });
        
        return result;
    }
}
