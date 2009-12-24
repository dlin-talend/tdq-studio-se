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
package org.talend.dataprofiler.core.ui.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.talend.cwm.builders.XMLSchemaBuilder;
import org.talend.cwm.exception.TalendException;
import org.talend.cwm.helper.CatalogHelper;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.ColumnSetHelper;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.cwm.helper.ModelElementHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.management.api.DqRepositoryViewService;
import org.talend.cwm.relational.TdCatalog;
import org.talend.cwm.relational.TdColumn;
import org.talend.cwm.relational.TdTable;
import org.talend.cwm.relational.TdView;
import org.talend.cwm.softwaredeployment.TdDataProvider;
import org.talend.cwm.xml.TdXMLElement;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.exception.MessageBoxExceptionHandler;
import org.talend.dataprofiler.core.helper.FolderNodeHelper;
import org.talend.dataprofiler.core.model.nodes.foldernode.NamedColumnSetFolderNode;
import org.talend.dataprofiler.core.ui.dialog.provider.DBTablesViewLabelProvider;
import org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage;
import org.talend.dataprofiler.core.ui.filters.DQFolderFliter;
import org.talend.dataprofiler.core.ui.filters.EMFObjFilter;
import org.talend.dataprofiler.core.ui.utils.ComparatorsFactory;
import org.talend.dataprofiler.core.ui.views.provider.DQRepositoryViewContentProvider;
import org.talend.dq.helper.EObjectHelper;
import org.talend.dq.helper.resourcehelper.PrvResourceFileHelper;
import org.talend.dq.nodes.foldernode.IFolderNode;
import org.talend.resource.ResourceManager;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.ColumnSet;
import orgomg.cwm.resource.relational.NamedColumnSet;

/**
 * @author Select the special columns from this dialog.
 * 
 */
public class ColumnsSelectionDialog extends TwoPartCheckSelectionDialog {

    private static Logger log = Logger.getLogger(ColumnsSelectionDialog.class);

    private Map<ModelElementKey, ModelElementCheckedMap> modelElementCheckedMap;

    private List<ModelElement> currentCheckedModelElement = new ArrayList<ModelElement>();

    private IFolder metadataFolder = ResourceManager.getMetadataFolder();

    public ColumnsSelectionDialog(AbstractAnalysisMetadataPage metadataFormPage, Shell parent, String title,
            List<? extends ModelElement> modelElementList, String message) {
        super(metadataFormPage, parent, message);
        modelElementCheckedMap = new HashMap<ModelElementKey, ModelElementCheckedMap>();
        initCheckedModelElement(modelElementList);

        addFilter(new EMFObjFilter());
        addFilter(new DQFolderFliter(true));

        setInput(metadataFolder);
        setTitle(title);
    }

    @Override
    /**
     * DOC mzhao bug 9240 mzhao 2009-11-05
     */
    protected void unfoldToCheckedElements() {
        Set<ModelElementKey> keySet = modelElementCheckedMap.keySet();
        ModelElementKey[] array = keySet.toArray(new ModelElementKey[keySet.size()]);
        for (ModelElementKey mek : array) {
            getTreeViewer().expandToLevel(mek.getModelElement(), 1);
            StructuredSelection structSel = new StructuredSelection(mek.getModelElement());
            getTreeViewer().setSelection(structSel);
        }
    }

    private void initCheckedModelElement(List<? extends ModelElement> modelElementList) {
        List<ModelElement> containerList = new ArrayList<ModelElement>();
        for (int i = 0; i < modelElementList.size(); i++) {
            ModelElement modelElement = modelElementList.get(i);
            modelElement.eContainer();
            ModelElement container = ModelElementHelper.getContainer(modelElement);
            if (!containerList.contains(container)) {
                containerList.add(container);
            }
            ModelElementKey modelElementKey = new ModelElementKeyImpl(container);
            ModelElementCheckedMap meCheckedMap = modelElementCheckedMap.get(modelElementKey);
            if (meCheckedMap == null) {
                meCheckedMap = new ModelElementCheckedMapImpl();
                this.modelElementCheckedMap.put(modelElementKey, meCheckedMap);
            }
            meCheckedMap.putModelElementChecked(modelElement, Boolean.TRUE);
        }
        this.setInitialElementSelections(containerList);
    }

    protected void initProvider() {
        fLabelProvider = new DBTablesViewLabelProvider();
        fContentProvider = new DBTreeViewContentProvider();
        sLabelProvider = new ColumnLabelProvider();
        sContentProvider = new ColumnContentProvider();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.dataprofiler.core.ui.dialog.TwoPartCheckSelectionDialog# addCheckedListener()
     */
    protected void addCheckedListener() {

        // When user checks a checkbox in the tree, check all its children
        getTreeViewer().addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {

                ColumnSelectionViewer columnViewer = (ColumnSelectionViewer) event.getSource();
                TreePath treePath = new TreePath(new Object[] { event.getElement() });
                columnViewer.setSelection(new TreeSelection(treePath));

                if (event.getChecked()) {
                    getTreeViewer().setSubtreeChecked(event.getElement(), true);
                    if (event.getElement() instanceof ColumnSet) {
                        setOutput(event.getElement());
                        handleColumnsChecked((ColumnSet) event.getElement(), true);
                    }

                } else {
                    getTreeViewer().setSubtreeChecked(event.getElement(), false);
                    if (event.getElement() instanceof ColumnSet) {
                        setOutput(event.getElement());
                        handleColumnsChecked((ColumnSet) event.getElement(), false);
                    }
                }
            }
        });

        getTableViewer().addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                if (event.getElement() instanceof TdColumn) {
                    handleColumnChecked((TdColumn) event.getElement(), event.getChecked());
                }
            }
        });
    }

    private ModelElement[] getCheckedModelElements(ModelElement modelElement) {
        boolean isColumnSet = modelElement instanceof ColumnSet;
        boolean isTdXMLElement = modelElement instanceof TdXMLElement;

        ModelElementKey mek = new ModelElementKeyImpl(modelElement);
        ModelElementCheckedMap meCheckMap = modelElementCheckedMap.get(mek);
        if (meCheckMap == null) {
            Boolean allCheckFlag = this.getTreeViewer().getChecked(modelElement);
            this.getTableViewer().setAllChecked(allCheckFlag);
            meCheckMap = new ModelElementCheckedMapImpl();
            ModelElement[] modelElements = null;
            if (isColumnSet) {
                modelElements = EObjectHelper.getColumns((ColumnSet) modelElement);
            } else if (isTdXMLElement) {
                TdXMLElement xmlElement = (TdXMLElement) modelElement;
                XMLSchemaBuilder schemaBuilder = XMLSchemaBuilder.getSchemaBuilder(xmlElement.getOwnedDocument());
                List<TdXMLElement> children = schemaBuilder.getChildren(xmlElement);
                modelElements = children.toArray(new ModelElement[children.size()]);
            }
            meCheckMap.putAllChecked(modelElements, allCheckFlag);
            modelElementCheckedMap.put(mek, meCheckMap);
            return allCheckFlag ? modelElements : null;
        } else {
            if (isColumnSet) {
                return meCheckMap.getCheckedModelElements(ColumnSetHelper.getColumns((ColumnSet) modelElement));
            } else if (isTdXMLElement) {
                TdXMLElement xmlElement = (TdXMLElement) modelElement;
                XMLSchemaBuilder schemaBuilder = XMLSchemaBuilder.getSchemaBuilder(xmlElement.getOwnedDocument());
                List<TdXMLElement> children = schemaBuilder.getChildren(xmlElement);
                return meCheckMap.getCheckedModelElements(children);
            }
            return null;
        }
    }

    private void handleColumnChecked(TdColumn column, Boolean checkedFlag) {
        ColumnSet columnSetOwner = ColumnHelper.getColumnSetOwner(column);
        if (checkedFlag) {
            getTreeViewer().setChecked(columnSetOwner, true);
        }
        ModelElementCheckedMap columnCheckMap = modelElementCheckedMap.get(new ModelElementKeyImpl(columnSetOwner));
        if (columnCheckMap != null) {
            columnCheckMap.putModelElementChecked(column, checkedFlag);
        }

    }

    private void handleColumnsChecked(ColumnSet columnSet, Boolean checkedFlag) {
        ModelElementKey key = new ModelElementKeyImpl(columnSet);
        ModelElementCheckedMap columnCheckMap = modelElementCheckedMap.get(key);
        if (columnCheckMap != null) {
            columnCheckMap.clear();
            columnCheckMap.putAllChecked(EObjectHelper.getColumns(columnSet), checkedFlag);
        } else {
            columnCheckMap = new ModelElementCheckedMapImpl();
            columnCheckMap.putAllChecked(EObjectHelper.getColumns(columnSet), checkedFlag);
            modelElementCheckedMap.put(key, columnCheckMap);
        }
        getTableViewer().setAllChecked(checkedFlag);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.dataprofiler.core.ui.dialog.TwoPartCheckSelectionDialog#
     * addSelectionButtonListener(org.eclipse.swt .widgets.Button, org.eclipse.swt.widgets.Button)
     */
    protected void addSelectionButtonListener(Button selectButton, Button deselectButton) {
        SelectionListener listener = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Object[] viewerElements = fContentProvider.getElements(getTreeViewer().getInput());
                if (fContainerMode) {
                    getTreeViewer().setCheckedElements(viewerElements);
                } else {
                    for (int i = 0; i < viewerElements.length; i++) {
                        getTreeViewer().setSubtreeChecked(viewerElements[i], true);
                    }
                }
                modelElementCheckedMap.clear();
                if (getTableViewer().getInput() != null) {
                    handleColumnsChecked((ColumnSet) getTableViewer().getInput(), true);
                }
                updateOKStatus();
            }
        };
        selectButton.addSelectionListener(listener);

        listener = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                getTreeViewer().setCheckedElements(new Object[0]);
                modelElementCheckedMap.clear();
                if (getTableViewer().getInput() != null) {
                    handleColumnsChecked((ColumnSet) getTableViewer().getInput(), false);
                }
                updateOKStatus();
            }
        };
        deselectButton.addSelectionListener(listener);
    }

    public void selectionChanged(SelectionChangedEvent event) {
        Object selectedObj = ((IStructuredSelection) event.getSelection()).getFirstElement();
        if (selectedObj instanceof ColumnSet || selectedObj instanceof TdXMLElement) {
            this.setOutput(selectedObj);
            ModelElement[] modelElements = getCheckedModelElements((ModelElement) selectedObj);
            if (modelElements != null) {
                this.getTableViewer().setCheckedElements(modelElements);
            }
        }

    }

    /**
     * DOC xqliu ColumnsSelectionDialog class global comment. Detailled comment
     */
    interface ModelElementKey {

        public int hashCode();

        public boolean equals(Object obj);

        public ModelElement getModelElement();

        public ModelElement getParentModelElement();
    }

    /**
     * DOC xqliu ColumnsSelectionDialog class global comment. Detailled comment
     */
    class ModelElementKeyImpl implements ModelElementKey {

        private static final int KEY_PRIME = 128;

        private ModelElement modelElement;

        private ModelElement parentModelElement;

        public ModelElementKeyImpl(ModelElement mElement) {
            modelElement = mElement;
            parentModelElement = getParentModelElement(mElement);
        }

        /**
         * DOC xqliu Comment method "getParentModelElement".
         * 
         * @param mElement
         * @return null if the mElement is the top element
         */
        private ModelElement getParentModelElement(ModelElement mElement) {
            if (mElement instanceof ColumnSet) {
                return EObjectHelper.getParent((ColumnSet) mElement);
            } else if (mElement instanceof TdXMLElement) {
                // TODO 10238
                TdXMLElement xmlElement = (TdXMLElement) mElement;
                xmlElement.getOwnedDocument();
            }
            return null;
        }

        public ModelElement getModelElement() {
            return modelElement;
        }

        public ModelElement getParentModelElement() {
            return parentModelElement;
        }

        public int hashCode() {
            return KEY_PRIME
                    + (getParentModelElement() == null ? 0 : new ModelElementKeyImpl(getParentModelElement()).hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ModelElementKey other = (ModelElementKey) obj;

            if (getModelElement() == null) {
                if (other.getModelElement() != null) {
                    return false;
                }
            } else if (!getModelElement().equals(other.getModelElement())) {
                return false;
            }

            if (this.getParentModelElement() == null) {
                if (other.getParentModelElement() != null) {
                    return false;
                }
            } else if (!getParentModelElement().equals(other.getParentModelElement())) {
                return false;
            }
            return true;
        }
    }

    /**
     * @author xqliu
     */
    interface ModelElementCheckedMap {

        public void putModelElementChecked(ModelElement modelElement, Boolean isChecked);

        public Boolean getModelElementChecked(ModelElement modelElement);

        public void putAllChecked(ModelElement[] modelElement, Boolean isChecked);

        public ModelElement[] getCheckedModelElements(List<? extends ModelElement> modelElementList);

        public List<ModelElement> getCheckedModelElementList(ModelElement modelElement);

        public void clear();
    }

    /**
     * DOC xqliu ColumnsSelectionDialog class global comment. Detailled comment
     */
    class ModelElementCheckedMapImpl implements ModelElementCheckedMap {

        Map<String, Boolean> modelElementNameMap = new HashMap<String, Boolean>();

        public void clear() {
            modelElementNameMap.clear();
        }

        public List<ModelElement> getCheckedModelElementList(ModelElement modelElement) {
            List<ModelElement> checkedModelElements = new ArrayList<ModelElement>();
            List<? extends ModelElement> modelElementList = new ArrayList<ModelElement>();
            
            if (modelElement instanceof ColumnSet) {
                modelElementList = ColumnSetHelper.getColumns((ColumnSet) modelElement);
            } else if ((modelElement instanceof TdXMLElement)) {
                TdXMLElement xmlElement = (TdXMLElement) modelElement;
                XMLSchemaBuilder schemaBuilder = XMLSchemaBuilder.getSchemaBuilder(xmlElement.getOwnedDocument());
                modelElementList = schemaBuilder.getChildren(xmlElement);
            }

            for (ModelElement mElement : modelElementList) {
                if (modelElementNameMap.containsKey(mElement.getName()) && modelElementNameMap.get(mElement.getName())) {
                    checkedModelElements.add(mElement);
                }
            }

            return checkedModelElements;
        }

        public ModelElement[] getCheckedModelElements(List<? extends ModelElement> modelElementList) {
            List<ModelElement> checkedModelElements = new ArrayList<ModelElement>();
            for (ModelElement modelElement : modelElementList) {
                if (modelElementNameMap.containsKey(modelElement.getName()) && modelElementNameMap.get(modelElement.getName())) {
                    checkedModelElements.add(modelElement);
                }
            }
            return checkedModelElements.toArray(new ModelElement[checkedModelElements.size()]);
        }

        public Boolean getModelElementChecked(ModelElement modelElement) {
            return modelElementNameMap.get(modelElement.getName());
        }

        public void putAllChecked(ModelElement[] modelElement, Boolean isChecked) {
            for (int i = 0; i < modelElement.length; i++) {
                modelElementNameMap.put(modelElement[i].getName(), isChecked);
            }
        }

        public void putModelElementChecked(ModelElement modelElement, Boolean isChecked) {
            modelElementNameMap.put(modelElement.getName(), isChecked);
        }

    }

    protected void computeResult() {
        setResult(getAllCheckedModelElements());
    }

    private List<ModelElement> getAllCheckedModelElements() {
        Object[] checkedNodes = this.getTreeViewer().getCheckedElements();
        List<ModelElement> meList = new ArrayList<ModelElement>();
        for (int i = 0; i < checkedNodes.length; i++) {
            if (!(checkedNodes[i] instanceof ColumnSet || checkedNodes[i] instanceof TdXMLElement)) {
                continue;
            }
            ModelElementKey mek = new ModelElementKeyImpl((ColumnSet) checkedNodes[i]);
            if (modelElementCheckedMap.containsKey(mek)) {
                ModelElementCheckedMap columnMap = modelElementCheckedMap.get(mek);
                meList.addAll(columnMap.getCheckedModelElementList((ModelElement) checkedNodes[i]));
            } else {
                if (checkedNodes[i] instanceof ColumnSet) {
                    meList.addAll(ColumnSetHelper.getColumns((ColumnSet) checkedNodes[i]));
                } else if ((checkedNodes[i] instanceof TdXMLElement)) {
                    TdXMLElement xmlElement = (TdXMLElement) checkedNodes[i];
                    XMLSchemaBuilder schemaBuilder = XMLSchemaBuilder.getSchemaBuilder(xmlElement.getOwnedDocument());
                    meList.addAll(schemaBuilder.getChildren(xmlElement));
                }
            }
        }
        return meList;
    }

    protected void okPressed() {
        super.okPressed();
        this.modelElementCheckedMap = null;
        this.currentCheckedModelElement = null;
    }

    /**
     * @author rli
     * 
     */
    class ColumnLabelProvider extends LabelProvider {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
         */
        public Image getImage(Object element) {
            return ImageLib.getImage(ImageLib.TD_COLUMN);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
         */
        public String getText(Object element) {
            TdColumn columnObj = (TdColumn) element;
            return columnObj.getName() + PluginConstant.SPACE_STRING + PluginConstant.PARENTHESIS_LEFT
                    + columnObj.getSqlDataType().getName() + PluginConstant.PARENTHESIS_RIGHT;
        }

    }

    /**
     * @author rli
     * 
     */
    class ColumnContentProvider implements IStructuredContentProvider {

        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof ColumnSet) {
                EObject eObj = (EObject) inputElement;
                ColumnSet columnSet = SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(eObj);
                if (columnSet != null) {
                    TdColumn[] columns = EObjectHelper.getColumns(columnSet);
                    if (columns.length <= 0) {
                        Package parentCatalogOrSchema = ColumnSetHelper.getParentCatalogOrSchema(columnSet);
                        if (parentCatalogOrSchema == null) {
                            return null;
                        }
                        TdDataProvider provider = DataProviderHelper.getTdDataProvider(parentCatalogOrSchema);
                        if (provider == null) {
                            return null;
                        }
                        try {
                            List<TdColumn> columnList = DqRepositoryViewService.getColumns(provider, columnSet, null, true);
                            columns = columnList.toArray(new TdColumn[columnList.size()]);
                            // store tables in catalog
                            // MOD scorreia 2009-01-29 columns are stored in the
                            // table
                            // ColumnSetHelper.addColumns(columnSet,
                            // columnList);
                        } catch (TalendException e) {
                            MessageBoxExceptionHandler.process(e);
                        }

                        PrvResourceFileHelper.getInstance().save(provider);
                    }
                    return sort(columns, ComparatorsFactory.MODELELEMENT_COMPARATOR_ID);
                }
            }
            return null;
        }

        /**
         * Sort the parameter objects, and return the sorted array.
         * 
         * @param objects
         * @param comparatorId the comparator id has been defined in the {@link ComparatorsFactory};
         * @return
         */
        @SuppressWarnings("unchecked")
        protected Object[] sort(Object[] objects, int comparatorId) {
            if (objects == null || objects.length <= 1) {
                return objects;
            }
            Arrays.sort(objects, ComparatorsFactory.buildComparator(comparatorId));
            return objects;
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        }

    }

    /**
     * DOC zqin ColumnsSelectionDialog class global comment. Detailled comment
     */
    class DBTreeViewContentProvider extends DQRepositoryViewContentProvider {

        /**
         * @param adapterFactory
         */
        public DBTreeViewContentProvider() {
            super();
        }

        @SuppressWarnings("unchecked")
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof IContainer) {
                IContainer container = ((IContainer) parentElement);
                IResource[] members = null;
                try {
                    members = container.members();
                } catch (CoreException e) {
                    log.error("Can't get the children of container:" + container.getLocation());
                }

                if (ResourceManager.getConnectionFolder().equals(container)
                        || ResourceManager.getMDMConnectionFolder().equals(container)) {
                    ComparatorsFactory.sort(members, ComparatorsFactory.FILEMODEL_COMPARATOR_ID);
                }
                return members;
            } else if (parentElement instanceof NamedColumnSet) {
                return null;
            } else if (parentElement instanceof NamedColumnSetFolderNode) {
                NamedColumnSetFolderNode folerNode = (NamedColumnSetFolderNode) parentElement;
                folerNode.loadChildren();
                Object[] children = folerNode.getChildren();
                if (children != null && children.length > 0) {
                    if (!(children[0] instanceof ColumnSet)) {
                        return children;
                    }
                    for (int i = 0; i < children.length; i++) {
                        ColumnSet columnSet = (ColumnSet) children[i];
                        ModelElementKey key = new ModelElementKeyImpl(columnSet);
                        if (modelElementCheckedMap.containsKey(key)) {
                            currentCheckedModelElement.add(columnSet);
                        }
                    }
                }
                return ComparatorsFactory.sort(children, ComparatorsFactory.MODELELEMENT_COMPARATOR_ID);
            }
            return super.getChildren(parentElement);
        }

        public Object getParent(Object element) {
            if (element instanceof EObject) {
                EObject eObj = (EObject) element;
                ColumnSet columnSet = SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(eObj);
                if (columnSet != null) {
                    IFolderNode folderNode = FolderNodeHelper.getFolderNode(EObjectHelper.getParent((ColumnSet) element),
                            columnSet);
                    return folderNode;
                }

                Package packageValue = SwitchHelpers.PACKAGE_SWITCH.doSwitch(eObj);
                TdCatalog parentCatalog = CatalogHelper.getParentCatalog(packageValue);
                if (parentCatalog != null) {
                    return parentCatalog;
                }

                if (packageValue != null) {
                    TdDataProvider tdDataProvider = DataProviderHelper.getTdDataProvider(packageValue);
                    IFile findCorrespondingFile = PrvResourceFileHelper.getInstance().findCorrespondingFile(tdDataProvider);
                    return findCorrespondingFile;
                    // Path path = new Path(uri.path());
                    // String fileName = path.lastSegment();
                    // IFolder connectionsFolder =
                    // ResourcesPlugin.getWorkspace().getRoot().getProject(
                    // DQStructureManager.getMetaData())
                    // .getFolder(DQStructureManager.DB_CONNECTIONS);
                    // IFile resourceFile = connectionsFolder.getFile(fileName);
                }
            } else if (element instanceof IFolderNode) {
                return ((IFolderNode) element).getParent();
            } else if (element instanceof IResource) {
                return ((IResource) element).getParent();
            }
            return super.getParent(element);
        }

        public boolean hasChildren(Object element) {
            return !(element instanceof TdView || element instanceof TdTable);
        }

    }

}
