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
package org.talend.dataprofiler.core.migration.impl;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.talend.commons.emf.EMFUtil;
import org.talend.dataprofiler.core.exception.ExceptionHandler;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.migration.AbstractMigrationTask;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.helpers.DataqualitySwitchHelper;
import org.talend.dq.helper.resourcehelper.AnaResourceFileHelper;
import org.talend.dq.helper.resourcehelper.DQRuleResourceFileHelper;
import org.talend.dq.helper.resourcehelper.PatternResourceFileHelper;
import org.talend.resource.ResourceManager;
import org.talend.resource.xml.TdqPropertieManager;
import orgomg.cwm.objectmodel.core.Dependency;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * 
 * DOC mzhao 2009-07-01 feature 7482.
 */
public class ReorderingLibraryFoldersTask extends AbstractMigrationTask {

    private static Logger log = Logger.getLogger(ReorderingLibraryFoldersTask.class);

    public ReorderingLibraryFoldersTask() {
    }

    public ReorderingLibraryFoldersTask(String id, String name, String version) {
        super(id, name, version);
    }

    public boolean execute() {
        if (log.isInfoEnabled()) {
            log.info(this.getName() + ": Reordering folders in Library");
        }
        IProject rootProject = ResourceManager.getRootProject();
        IFolder libraryFolder = rootProject.getFolder(ResourceManager.LIBRARIES_FOLDER_NAME);
        try {

            // Regex Patterns
            IFolder oldPatternFolder = libraryFolder.getFolder(DQStructureManager.PATTERNS);
            IFolder newPatternFolder = libraryFolder.getFolder(DQStructureManager.PATTERNS); 
            String folderProperty = DQStructureManager.PATTERNS_FOLDER_PROPERTY;
            IFolder newRegexSubfolder = createSubfolder(newPatternFolder, DQStructureManager.REGEX, folderProperty);
            movePatternsIntoPatternsRegex(oldPatternFolder, newRegexSubfolder, folderProperty);
            // oldPatternFolder.delete(true, null); // Do not delete because it's the same as before
            
            // SQL Patterns
            IFolder oldSqlPatternsFolder = libraryFolder.getFolder(DQStructureManager.SQL_PATTERNS);
            IFolder newSqlSubfolder = DQStructureManager.getInstance().createNewFoler(newPatternFolder, DQStructureManager.SQL);
            folderProperty = DQStructureManager.SQLPATTERNS_FOLDER_PROPERTY;
            movePatternsIntoPatternsRegex(oldSqlPatternsFolder, newSqlSubfolder, folderProperty);
            oldSqlPatternsFolder.delete(true, null);
            
            // DQ Rules
            IFolder oldDqRulesFolder = libraryFolder.getFolder(DQStructureManager.DQ_RULES);
            IFolder newRulesFolder = createSubfolder(libraryFolder, DQStructureManager.RULES, folderProperty); 
            folderProperty = DQStructureManager.DQRULES_FOLDER_PROPERTY;
            IFolder newRulesSQLSubfolder = createSubfolder(newRulesFolder, DQStructureManager.SQL, folderProperty);
            movePatternsIntoPatternsRegex(oldDqRulesFolder, newRulesSQLSubfolder, folderProperty);
            oldDqRulesFolder.delete(true, null);

            // Refresh project
            rootProject.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            ExceptionHandler.process(e);
            return false;
        }

        return true;
    }

    /**
     * DOC scorreia Comment method "createSubfolder".
     * @param newPatternFolder
     * @return
     * @throws CoreException
     */
    private IFolder createSubfolder(IFolder newPatternFolder, final String folderName, String folderProp) throws CoreException {
        TdqPropertieManager.getInstance().addFolderProperties(newPatternFolder, DQStructureManager.FOLDER_CLASSIFY_KEY,
                folderProp);
        return DQStructureManager.getInstance().createNewFoler(newPatternFolder, folderName);
    }

    private void movePatternsIntoPatternsRegex(IFolder oldSubFolder, IFolder newSubfolder, final String folderProperty)
            throws CoreException {
        
        TdqPropertieManager.getInstance().addFolderProperties(newSubfolder, DQStructureManager.FOLDER_CLASSIFY_KEY,
                folderProperty);
        
        for (IResource oldResource : oldSubFolder.members()) {
            if (newSubfolder.getName().equals(oldResource.getName())) {
                continue;
            }
            
            // cannot simply copy EMF files: need to keep the links between files when moving them. See bug 9461
            if (oldResource instanceof IFolder) {
                IFolder oldFolder = (IFolder) oldResource;
                
                IFolder newFolder = DQStructureManager.getInstance().createNewFoler(newSubfolder, oldFolder.getName());
                TdqPropertieManager.getInstance().addFolderProperties(newFolder, DQStructureManager.FOLDER_CLASSIFY_KEY,
                        folderProperty);
                
                movePatternsIntoPatternsRegex(oldFolder, newFolder, folderProperty);
                // delete folder
                oldFolder.delete(true, null);
            }

            if (oldResource instanceof IFile) {
                IFile file = (IFile) oldResource;
                final ModelElement pattern = getModelElement(file, folderProperty);
                final EList<Dependency> supplierDependency = pattern.getSupplierDependency();
                if (supplierDependency.isEmpty()) {
                    // simple copy of file is enough
                    oldResource.copy(newSubfolder.getFolder(oldResource.getName()).getFullPath(), true, null);
                } else {
                    // handle dependent analyses
                    for (Dependency dependency : supplierDependency) {
                        URI newUri = URI.createPlatformResourceURI(newSubfolder.getFullPath().toOSString(), true);
                        // move pattern
                        EMFUtil.changeUri(pattern.eResource(), newUri);
                        final EList<ModelElement> clientAnalyses = dependency.getClient();
                        for (ModelElement modelElement : clientAnalyses) {
                            Analysis analysis = DataqualitySwitchHelper.ANALYSIS_SWITCH.doSwitch(modelElement);
                            if (analysis != null) {
                                AnaResourceFileHelper.getInstance().save(analysis);
                            }
                        }
                    }
                }
                oldResource.delete(true, null);
            }  
        }
    }

    /**
     * DOC scorreia Comment method "getEMFObject".
     * 
     * @param file
     * @param folderProperty
     * @return
     */
    private ModelElement getModelElement(IFile file, String folderProperty) {
        if (StringUtils.equals(folderProperty, DQStructureManager.PATTERNS_FOLDER_PROPERTY)
                || StringUtils.equals(folderProperty, DQStructureManager.SQLPATTERNS_FOLDER_PROPERTY)) {
            return PatternResourceFileHelper.getInstance().findPattern(file);
        } else if (StringUtils.equals(folderProperty, DQStructureManager.DQRULES_FOLDER_PROPERTY)) {
            return DQRuleResourceFileHelper.getInstance().findWhereRule(file);
        }
        log.error("Unhandled folder property " + folderProperty);
        return null;
        
    }

    public MigrationTaskType getMigrationTaskType() {
        return MigrationTaskType.STUCTRUE;
    }

    public Date getOrder() {
        Calendar calender = Calendar.getInstance();
        calender.set(2009, 07, 01);
        return calender.getTime();
    }

}
