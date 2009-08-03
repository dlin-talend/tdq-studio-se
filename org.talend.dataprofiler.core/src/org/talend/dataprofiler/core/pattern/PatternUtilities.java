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
package org.talend.dataprofiler.core.pattern;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.cwm.dependencies.DependenciesHandler;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.management.connection.DatabaseContentRetriever;
import org.talend.cwm.management.connection.JavaSqlFactory;
import org.talend.cwm.softwaredeployment.TdDataProvider;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.model.ColumnIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.domain.pattern.ExpressionType;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.domain.pattern.PatternComponent;
import org.talend.dataquality.domain.pattern.impl.RegularExpressionImpl;
import org.talend.dataquality.factories.PatternIndicatorFactory;
import org.talend.dataquality.helpers.DomainHelper;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.PatternMatchingIndicator;
import org.talend.dataquality.indicators.definition.IndicatorDefinition;
import org.talend.dq.dbms.DbmsLanguage;
import org.talend.dq.dbms.DbmsLanguageFactory;
import org.talend.dq.helper.resourcehelper.PatternResourceFileHelper;
import org.talend.dq.indicators.definitions.DefinitionHandler;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import org.talend.resource.ResourceManager;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.foundation.softwaredeployment.DataManager;
import orgomg.cwm.foundation.softwaredeployment.SoftwareSystem;

/**
 * DOC qzhang class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40Z nrousseau $
 * 
 */
public final class PatternUtilities {

    private static Logger log = Logger.getLogger(PatternUtilities.class);

    private PatternUtilities() {
    }

    /**
     * DOC qzhang Comment method "isLibraiesSubfolder".
     * 
     * @param folder
     * @param subs
     * @return
     */
    public static boolean isLibraiesSubfolder(IFolder folder, String... subs) {
        for (String sub : subs) {
            // MOD mzhao 2009-04-02,DQ repository structure changed.
            IPath path = new Path(ResourceManager.getRootProjectName() + File.separator + ResourceManager.LIBRARIES_FOLDER_NAME);
            path = path.append(sub);
            IPath fullPath = folder.getFullPath();
            boolean prefixOf = path.isPrefixOf(fullPath);
            if (prefixOf) {
                return prefixOf;
            }
        }
        return false;
    }

    /**
     * DOC qzhang Comment method "isPatternValid".
     * 
     * @param pattern
     * @return
     */
    public static boolean isPatternValid(Pattern pattern) {
        boolean valid = true;
        EList<PatternComponent> components = pattern.getComponents();
        for (int i = 0; i < components.size(); i++) {
            RegularExpressionImpl regularExpress = (RegularExpressionImpl) components.get(i);
            String body = regularExpress.getExpression().getBody();
            valid = ((body != null) && body.matches("'.*'")); //$NON-NLS-1$
            if (!valid) {
                break;
            }
        }
        return valid;
    }

    /**
     * DOC qzhang Comment method "createIndicatorUnit".
     * 
     * @param pfile
     * @param columnIndicator
     * @param analysis
     * @return
     */
    public static IndicatorUnit createIndicatorUnit(IFile pfile, ColumnIndicator columnIndicator, Analysis analysis) {
        return createIndicatorUnit(pfile, columnIndicator, analysis, null);
    }

    /**
     * DOC xqliu Comment method "createIndicatorUnit".
     * 
     * @param pfile
     * @param columnIndicator
     * @param analysis
     * @param indicatorDefinition
     * @return
     */
    public static IndicatorUnit createIndicatorUnit(IFile pfile, ColumnIndicator columnIndicator, Analysis analysis,
            IndicatorDefinition indicatorDefinition) {
        Pattern pattern = PatternResourceFileHelper.getInstance().findPattern(pfile);

        for (Indicator indicator : columnIndicator.getIndicators()) {
            if (pattern.getName().equals(indicator.getName())) {
                return null;
            }
        }

        // MOD scorreia 2009-01-06: when expression type is not set (version
        // TOP-1.1.x), then it's supposed to be a
        // regexp pattern. This could be false because expression type was not
        // set into SQL pattern neither in TOP-1.1.
        // This means that there could exist the need for a migration task to
        // set the expression type depending on the
        // folder where the pattern is stored. The method
        // DomainHelper.getExpressionType(pattern) tries to find the type
        // of pattern.
        String expressionType = DomainHelper.getExpressionType(pattern);
        boolean isSQLPattern = (ExpressionType.SQL_LIKE.getLiteral().equals(expressionType));
        PatternMatchingIndicator patternMatchingIndicator = isSQLPattern ? PatternIndicatorFactory
                .createSqlPatternMatchingIndicator(pattern) : PatternIndicatorFactory.createRegexpMatchingIndicator(pattern);

        DbmsLanguage dbmsLanguage = DbmsLanguageFactory.createDbmsLanguage(analysis);
        if (ExpressionType.REGEXP.getLiteral().equals(expressionType) && dbmsLanguage.getRegexp(pattern) == null) {
            // this is when we must tell the user that no regular expression
            // exists for the selected database
            MessageDialogWithToggle.openInformation(null,
                    DefaultMessagesImpl.getString("PatternUtilities.Pattern"), DefaultMessagesImpl //$NON-NLS-1$
                            .getString("PatternUtilities.noRegexForDB")); //$NON-NLS-1$

            return null;
        }
        // TODO Currently the previous condition checks only whether there exist
        // a regular expression for the analyzed
        // database, but we probably test also whether the analyzed database
        // support the regular expressions (=> check
        // DB type, DB number version, existence of UDF)
        DataManager dm = analysis.getContext().getConnection();
        if (dm != null) {
            TypedReturnCode<Connection> trc = JavaSqlFactory.createConnection((TdDataProvider) dm);

            if (trc != null) {
                Connection conn = trc.getObject();

                try {
                    SoftwareSystem softwareSystem = DatabaseContentRetriever.getSoftwareSystem(conn);
                    dbmsLanguage = DbmsLanguageFactory.createDbmsLanguage(softwareSystem);
                } catch (SQLException e) {
                    log.error(e, e);
                }
            }

            if (!(dbmsLanguage.supportRegexp() || isDBDefinedUDF(dbmsLanguage))) {
                MessageDialogWithToggle.openInformation(null,
                        DefaultMessagesImpl.getString("PatternUtilities.Pattern"), DefaultMessagesImpl //$NON-NLS-1$
                                .getString("PatternUtilities.couldnotSetIndicator")); //$NON-NLS-1$
                return null;
            }
        }

        // MOD scorreia 2008-09-18: bug 5131 fixed: set indicator's definition
        // when the indicator is created.
        if (indicatorDefinition == null) {
            if (!DefinitionHandler.getInstance().setDefaultIndicatorDefinition(patternMatchingIndicator)) {
                log.error("Could not set the definition of the given indicator :" + patternMatchingIndicator.getName()); //$NON-NLS-1$
            }
        } else {
            patternMatchingIndicator.setIndicatorDefinition(indicatorDefinition);
        }

        IndicatorEnum type = IndicatorEnum.findIndicatorEnum(patternMatchingIndicator.eClass());
        IndicatorUnit addIndicatorUnit = columnIndicator.addSpecialIndicator(type, patternMatchingIndicator);
        DependenciesHandler.getInstance().setUsageDependencyOn(analysis, pattern);
        return addIndicatorUnit;
    }

    /**
     * DOC bzhou Comment method "isDBDefinedUDF".
     * 
     * This method is to check if user have defined the related funciton to this database type.
     * 
     * @param dbmsLanguage
     * @return
     */
    private static boolean isDBDefinedUDF(DbmsLanguage dbmsLanguage) {
        Preferences prefers = ResourcesPlugin.getPlugin().getPluginPreferences();
        if (prefers != null) {
            String udfValue = prefers.getString(dbmsLanguage.getDbmsName());
            if (udfValue != null && !"".equals(udfValue)) { //$NON-NLS-1$
                return true;
            }
        }
        return false;
    }

    public static Set<String> getAllPatternNames(IFolder folder) {

        Set<String> list = new HashSet<String>();
        return getNestFolderPatternNames(list, folder);
    }

    /**
     * DOC zqin Comment method "getNestFolderPatternNames".
     * 
     * @param folder
     * @return
     */
    private static Set<String> getNestFolderPatternNames(Set<String> list, IFolder folder) {
        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile) {
                    Pattern fr = PatternResourceFileHelper.getInstance().findPattern((IFile) resource);
                    if (fr != null) {
                        list.add(fr.getName());
                    }
                } else {
                    getNestFolderPatternNames(list, (IFolder) resource);
                }
            }
        } catch (CoreException e) {
            log.error(e, e);
        }
        return list;
    }

    public static IFile[] getPatternFileByIndicator(ColumnIndicator clmIndicator) {
        Indicator[] patternIndicators = clmIndicator.getPatternIndicators();
        List<IFile> existedPatternFiles = new ArrayList<IFile>();

        if (patternIndicators.length != 0) {
            for (Indicator patternIndicator : patternIndicators) {
                PatternMatchingIndicator ptnIndicaotr = (PatternMatchingIndicator) patternIndicator;
                List<Pattern> patterns = ptnIndicaotr.getParameters().getDataValidDomain().getPatterns();
                for (Pattern pattern : patterns) {
                    for (IFile file : getAllPatternFiles()) {
                        Pattern fpattern = PatternResourceFileHelper.getInstance().findPattern(file);
                        if (pattern.getName().equals(fpattern.getName())) {
                            existedPatternFiles.add(file);
                        }
                    }
                }
            }
        }

        return existedPatternFiles.toArray(new IFile[existedPatternFiles.size()]);
    }

    private static Set<IFile> getNestedPatternFiles(Set<IFile> list, IFolder folder) {
        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    if (file.getFileExtension().equals(FactoriesUtil.PATTERN)) {
                        list.add((IFile) resource);
                    }
                } else {
                    getNestedPatternFiles(list, (IFolder) resource);
                }
            }
        } catch (CoreException e) {
            log.error(e, e);
        }

        return list;
    }

    private static List<IFile> getAllPatternFiles() {
        List<IFile> patternFiles = new ArrayList<IFile>();

        IFolder pfolder = ResourceManager.getLibrariesFolder().getFolder(DQStructureManager.PATTERNS);
        IFolder sfolder = ResourceManager.getLibrariesFolder().getFolder(DQStructureManager.SQL_PATTERNS);

        Set<IFile> list = new HashSet<IFile>();
        patternFiles.addAll(getNestedPatternFiles(list, pfolder));
        patternFiles.addAll(getNestedPatternFiles(list, sfolder));

        return patternFiles;
    }

    /**
     * DOC xqliu Comment method "createPatternCheckedTreeSelectionDialog".
     * 
     * @param libProject
     * @return
     */
    public static CheckedTreeSelectionDialog createPatternCheckedTreeSelectionDialog(IFolder libProject) {
        CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(null, new PatternLabelProvider(),
                new WorkbenchContentProvider());
        dialog.setInput(libProject);
        dialog.setValidator(new ISelectionStatusValidator() {

            public IStatus validate(Object[] selection) {
                IStatus status = Status.OK_STATUS;
                for (Object patte : selection) {
                    if (patte instanceof IFile) {
                        IFile file = (IFile) patte;
                        if (FactoriesUtil.PATTERN.equals(file.getFileExtension())) {
                            Pattern findPattern = PatternResourceFileHelper.getInstance().findPattern(file);
                            boolean validStatus = TaggedValueHelper.getValidStatus(findPattern);
                            if (!validStatus) {
                                status = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, DefaultMessagesImpl
                                        .getString("AnalysisColumnTreeViewer.chooseValidPatterns")); //$NON-NLS-1$
                            }
                        }
                    }
                }
                return status;
            }

        });
        dialog.addFilter(new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IFile) {
                    IFile file = (IFile) element;
                    if (FactoriesUtil.PATTERN.equals(file.getFileExtension())) {
                        return true;
                    }
                } else if (element instanceof IFolder) {
                    IFolder folder = (IFolder) element;
                    return PatternUtilities.isLibraiesSubfolder(folder, DQStructureManager.PATTERNS,
                            DQStructureManager.SQL_PATTERNS);
                }
                return false;
            }
        });
        dialog.setContainerMode(true);
        dialog.setTitle(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.patternSelector")); //$NON-NLS-1$
        dialog.setMessage(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.patterns")); //$NON-NLS-1$
        dialog.setSize(80, 30);
        return dialog;
    }

}

/**
 * DOC zqin AnalysisColumnTreeViewer class global comment. Detailled comment
 */
class PatternLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof IFolder) {
            return ImageLib.getImage(ImageLib.FOLDERNODE_IMAGE);
        }

        if (element instanceof IFile) {
            Pattern findPattern = PatternResourceFileHelper.getInstance().findPattern((IFile) element);
            boolean validStatus = TaggedValueHelper.getValidStatus(findPattern);
            ImageDescriptor imageDescriptor = ImageLib.getImageDescriptor(ImageLib.PATTERN_REG);
            if (!validStatus) {
                ImageDescriptor warnImg = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_OBJS_WARN_TSK);
                DecorationOverlayIcon icon = new DecorationOverlayIcon(imageDescriptor.createImage(), warnImg,
                        IDecoration.BOTTOM_RIGHT);
                imageDescriptor = icon;
            }
            return imageDescriptor.createImage();
        }

        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof IFile) {
            IFile file = (IFile) element;
            Pattern pattern = PatternResourceFileHelper.getInstance().findPattern(file);
            if (pattern != null) {
                return pattern.getName();
            }
        }

        if (element instanceof IFolder) {
            return ((IFolder) element).getName();
        }

        return ""; //$NON-NLS-1$
    }
}
