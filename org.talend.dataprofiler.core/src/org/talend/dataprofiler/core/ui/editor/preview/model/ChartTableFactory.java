// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.editor.preview.model;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PartInitException;
import org.jfree.util.Log;
import org.talend.commons.utils.platform.PluginChecker;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.db.connection.ConnectionUtils;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.pattern.actions.CreatePatternAction;
import org.talend.dataprofiler.core.service.GlobalServiceRegister;
import org.talend.dataprofiler.core.service.IDatabaseJobService;
import org.talend.dataprofiler.core.service.IJobService;
import org.talend.dataprofiler.core.ui.editor.analysis.drilldown.DrillDownEditorInput;
import org.talend.dataprofiler.core.ui.utils.TableUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.AnalysisType;
import org.talend.dataquality.analysis.AnalyzedDataSet;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.domain.pattern.ExpressionType;
import org.talend.dataquality.indicators.DatePatternFreqIndicator;
import org.talend.dataquality.indicators.DistinctCountIndicator;
import org.talend.dataquality.indicators.DuplicateCountIndicator;
import org.talend.dataquality.indicators.FrequencyIndicator;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.PatternFreqIndicator;
import org.talend.dataquality.indicators.PatternLowFreqIndicator;
import org.talend.dataquality.indicators.PatternMatchingIndicator;
import org.talend.dataquality.indicators.PossiblePhoneCountIndicator;
import org.talend.dataquality.indicators.UniqueCountIndicator;
import org.talend.dataquality.indicators.ValidPhoneCountIndicator;
import org.talend.dataquality.indicators.WellFormE164PhoneCountIndicator;
import org.talend.dataquality.indicators.WellFormIntePhoneCountIndicator;
import org.talend.dataquality.indicators.WellFormNationalPhoneCountIndicator;
import org.talend.dataquality.indicators.columnset.AllMatchIndicator;
import org.talend.dataquality.indicators.columnset.util.ColumnsetSwitch;
import org.talend.dataquality.indicators.util.IndicatorsSwitch;
import org.talend.dq.analysis.explore.IDataExplorer;
import org.talend.dq.dbms.DbmsLanguage;
import org.talend.dq.dbms.DbmsLanguageFactory;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.pattern.PatternTransformer;
import org.talend.resource.ResourceManager;

/**
 * DOC zqin class global comment. Detailled comment
 */
public final class ChartTableFactory {

    private ChartTableFactory() {
    }

    public static void addMenuAndTip(final TableViewer tbViewer, final IDataExplorer explorer, final Analysis analysis) {

        final ExecutionLanguage currentEngine = analysis.getParameters().getExecutionLanguage();
        final boolean isJAVALanguage = ExecutionLanguage.JAVA == currentEngine;
        final Connection tdDataProvider = (Connection) analysis.getContext().getConnection();
        final boolean isMDMAnalysis = ConnectionUtils.isMdmConnection(tdDataProvider);
        final boolean isDelimitedFileAnalysis = ConnectionUtils.isDelimitedFileConnection(tdDataProvider);

        final Table table = tbViewer.getTable();

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent e) {
                // MOD xqliu 2009-05-11 bug 6561
                if (table.getMenu() != null) {
                    table.getMenu().setVisible(false);
                }

                if (e.button == 3) {

                    StructuredSelection selection = (StructuredSelection) tbViewer.getSelection();
                    final ChartDataEntity dataEntity = (ChartDataEntity) selection.getFirstElement();
                    final Indicator indicator = dataEntity != null ? dataEntity.getIndicator() : null;

                    if (indicator != null && dataEntity != null) {
                        Menu menu = new Menu(table.getShell(), SWT.POP_UP);
                        table.setMenu(menu);
                        if (!isJAVALanguage) {
                            MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis, dataEntity);
                            for (final MenuItemEntity itemEntity : itemEntities) {
                                MenuItem item = new MenuItem(menu, SWT.PUSH);
                                item.setText(itemEntity.getLabel());
                                item.setImage(itemEntity.getIcon());

                                item.addSelectionListener(new SelectionAdapter() {

                                    @Override
                                    public void widgetSelected(SelectionEvent e) {
                                        String query = itemEntity.getQuery();
                                        String editorName = indicator.getName();
                                        CorePlugin.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                                    }
                                });

                                if (isPatternFrequencyIndicator(indicator)) {
                                    MenuItem itemCreatePatt = new MenuItem(menu, SWT.PUSH);
                                    itemCreatePatt.setText(DefaultMessagesImpl
                                            .getString("ChartTableFactory.GenerateRegularPattern")); //$NON-NLS-1$
                                    itemCreatePatt.setImage(ImageLib.getImage(ImageLib.PATTERN_REG));
                                    itemCreatePatt.addSelectionListener(new SelectionAdapter() {

                                        @Override
                                        public void widgetSelected(SelectionEvent e) {
                                            DbmsLanguage language = DbmsLanguageFactory.createDbmsLanguage(analysis);
                                            PatternTransformer pattTransformer = new PatternTransformer(language);
                                            createPattern(analysis, itemEntity, pattTransformer);
                                        }
                                    });
                                }
                            }
                        } else {
                            try {
                                AnalyzedDataSet analyDataSet = analysis.getResults().getIndicToRowMap().get(indicator);
                                if (analysis.getParameters().isStoreData()) {
                                    // MOD gdbu 2011-7-12 bug : 22524
                                    if (!(analyDataSet != null && (analyDataSet.getData() != null
                                            && analyDataSet.getData().size() > 0 || analyDataSet.getFrequencyData() != null
                                            && analyDataSet.getFrequencyData().size() > 0 || analyDataSet.getPatternData() != null
                                            && analyDataSet.getPatternData().size() > 0))) {
                                        return;
                                    }
                                    // ~22524
                                        MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis,
                                                dataEntity);
                                        for (final MenuItemEntity itemEntity : itemEntities) {
                                            MenuItem item = new MenuItem(menu, SWT.PUSH);
                                            item.setText(itemEntity.getLabel());
                                            item.setImage(itemEntity.getIcon());
                                            item.addSelectionListener(new SelectionAdapter() {

                                                @Override
                                                public void widgetSelected(SelectionEvent e) {
                                                    try {
                                                        CorePlugin
                                                                .getDefault()
                                                                .getWorkbench()
                                                                .getActiveWorkbenchWindow()
                                                                .getActivePage()
                                                                .openEditor(
                                                                        new DrillDownEditorInput(analysis, dataEntity, itemEntity),
                                                                        "org.talend.dataprofiler.core.ui.editor.analysis.drilldown.drillDownResultEditor");//$NON-NLS-1$
                                                    } catch (PartInitException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }

                                            });
                                            if (isPatternFrequencyIndicator(indicator)) {
                                                if (itemEntity.getQuery() == null) {
                                                    itemEntity.setQuery(dataEntity.getKey().toString());

                                                }
                                                MenuItem itemCreatePatt = new MenuItem(menu, SWT.PUSH);
                                                itemCreatePatt.setText(DefaultMessagesImpl
                                                        .getString("ChartTableFactory.GenerateRegularPattern")); //$NON-NLS-1$
                                                itemCreatePatt.setImage(ImageLib.getImage(ImageLib.PATTERN_REG));
                                                itemCreatePatt.addSelectionListener(new SelectionAdapter() {

                                                    @Override
                                                    public void widgetSelected(SelectionEvent e) {
                                                        DbmsLanguage language = DbmsLanguageFactory.createDbmsLanguage(analysis);
                                                        PatternTransformer pattTransformer = new PatternTransformer(language);
                                                        createPattern(analysis, itemEntity, pattTransformer);
                                                    }
                                                });
                                            }
                                        }

                                }
                            } catch (NullPointerException nullexception) {

                                Log.error("drill down the data shuold run the analysis firstly." + nullexception);//$NON-NLS-1$
                            }
                            // MOD by zshen feature 11574:add menu "Generate regular pattern" to date pattern
                            if (isDatePatternFrequencyIndicator(indicator)) {
                                final DatePatternFreqIndicator dateIndicator = (DatePatternFreqIndicator) indicator;
                                MenuItem itemCreatePatt = new MenuItem(menu, SWT.PUSH);
                                itemCreatePatt.setText(DefaultMessagesImpl.getString("ChartTableFactory.GenerateRegularPattern")); //$NON-NLS-1$
                                itemCreatePatt.setImage(ImageLib.getImage(ImageLib.PATTERN_REG));
                                itemCreatePatt.addSelectionListener(new SelectionAdapter() {

                                    @Override
                                    public void widgetSelected(SelectionEvent e) {
                                        DbmsLanguage language = DbmsLanguageFactory.createDbmsLanguage(analysis);
                                        IFolder folder = ResourceManager.getPatternRegexFolder();
                                        String model = dataEntity.getLabel();
                                        String regex = dateIndicator.getRegex(model);
                                        new CreatePatternAction(
                                                folder,
                                                ExpressionType.REGEXP,
                                                "'" + regex + "'", model == null ? "" : "match \"" + model + "\"", language.getDbmsName()).run(); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$ 
                                    }

                                });
                            }
                        }
                        if (PluginChecker.isTDCPLoaded() && !isMDMAnalysis && !isDelimitedFileAnalysis) {
                            final IDatabaseJobService service = (IDatabaseJobService) GlobalServiceRegister.getDefault()
                                    .getService(IJobService.class);
                            if (service != null) {
                                service.setIndicator(indicator);
                                service.setAnalysis(analysis);
                                MenuItem item = null;
                                if (isDUDIndicator(indicator)
                                        && AnalysisType.COLUMN_SET != analysis.getParameters().getAnalysisType()) {
                                    item = new MenuItem(menu, SWT.PUSH);
                                    item.setText(DefaultMessagesImpl.getString("ChartTableFactory.RemoveDuplicate")); //$NON-NLS-1$
                                } else if (isPatternMatchingIndicator(indicator)) {
                                    item = new MenuItem(menu, SWT.PUSH);
                                    item.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.generateJob"));//$NON-NLS-1$ 
                                } else if (isAllMatchIndicator(indicator)) {
                                    item = new MenuItem(menu, SWT.PUSH);
                                    item.setText("Generate an ETL job to handle rows");//$NON-NLS-1$ 
                                } else if (isPhonseNumberIndicator(indicator)) {
                                    item = new MenuItem(menu, SWT.PUSH);
                                    item.setText("Generate a standardization phone number job");
                                }

                                if (item != null) {
                                    item.setImage(ImageLib.getImage(ImageLib.ICON_PROCESS));
                                    item.addSelectionListener(getAdapter(service));
                                }
                            }
                        }

                        // ~11574
                        menu.setVisible(true);
                    }
                }
            }

            private SelectionAdapter getAdapter(final IDatabaseJobService service) {
                return new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        service.executeJob();
                    }
                };
            }
        });

        // add tool tip
        TableUtils.addTooltipOnTableItem(table);
    }

    /**
     * DOC bZhou Comment method "createPattern".
     * 
     * @param analysis
     * @param itemEntity
     * @param pattTransformer
     */
    public static void createPattern(Analysis analysis, MenuItemEntity itemEntity, final PatternTransformer pattTransformer) {
        String language = pattTransformer.getDbmsLanguage().getDbmsName();
        String query = itemEntity.getQuery();

        if (analysis.getParameters().getExecutionLanguage().compareTo(ExecutionLanguage.SQL) == 0) {
            query = query.substring(query.indexOf('=') + 3, query.lastIndexOf(')') - 1);//$NON-NLS-1$ //$NON-NLS-2$
        }
        String regex = pattTransformer.getRegexp(query);
        IFolder folder = ResourceManager.getPatternRegexFolder();
        new CreatePatternAction(folder, ExpressionType.REGEXP, "'" + regex + "'", language).run(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * DOC bZhou Comment method "isAllMatchIndicator".
     * 
     * @param indicator
     * @return
     */
    public static boolean isAllMatchIndicator(Indicator indicator) {
        ColumnsetSwitch<Indicator> iSwitch = new ColumnsetSwitch<Indicator>() {

            @Override
            public Indicator caseAllMatchIndicator(AllMatchIndicator object) {
                return object;
            }
        };
        return iSwitch.doSwitch(indicator) != null;
    }

    /**
     * DOC bZhou Comment method "isDUDIndicator".
     * 
     * @param indicator
     * @return false if the indicator is not Duplicated,Uniqure,Distinct indicator.
     */
    public static boolean isDUDIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {

            public Indicator caseDuplicateCountIndicator(DuplicateCountIndicator object) {
                return object;
            };

            public Indicator caseUniqueCountIndicator(UniqueCountIndicator object) {
                return object;
            };

            public Indicator caseDistinctCountIndicator(DistinctCountIndicator object) {
                return object;
            };
        };

        return iSwitch.doSwitch(indicator) != null;
    }

    /**
     * DOC bZhou Comment method "isPatternMatchingIndicator".
     * 
     * @param indicator
     * @return false if the indicator is not pattern matching indicator.
     */
    public static boolean isPatternMatchingIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {

            @Override
            public Indicator casePatternMatchingIndicator(PatternMatchingIndicator object) {
                return object;
            }
        };

        return iSwitch.doSwitch(indicator) != null;
    }

    /**
     * DOC zshen Comment method "isFrequenceIndicator".
     * 
     * @param indicator
     * @return false if the indicator is not Frequence indicator.
     */

    public static boolean isFrequenceIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {

            @Override
            public Indicator caseFrequencyIndicator(FrequencyIndicator object) {
                return object;
            }
        };
        return iSwitch.doSwitch(indicator) != null;
    }

    /**
     * DOC bZhou Comment method "isPatternFrequencyIndicator".
     * 
     * @param indicator
     * @return false if the indicator is not pattern frequency indicator.
     */
    public static boolean isPatternFrequencyIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {

            @Override
            public Indicator casePatternFreqIndicator(PatternFreqIndicator object) {
                return object;
            }

            @Override
            public Indicator casePatternLowFreqIndicator(PatternLowFreqIndicator object) {
                return object;
            }
        };

        return iSwitch.doSwitch(indicator) != null;
    }

    /**
     * DOC zshen Comment method "isDatePatternFrequencyIndicator".
     * 
     * @param indicator
     * @return false if the indicator is not Date pattern frequency indicator.
     */
    public static boolean isDatePatternFrequencyIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {

            @Override
            public Indicator caseDatePatternFreqIndicator(DatePatternFreqIndicator object) {
                return object;
            }
        };

        return iSwitch.doSwitch(indicator) != null;
    }
    
    /**
     * DOC Administrator Comment method "isPhonseNumberIndicator".
     * 
     * @param indicator
     * @return
     */
    public static boolean isPhonseNumberIndicator(Indicator indicator) {
        IndicatorsSwitch<Indicator> iSwitch = new IndicatorsSwitch<Indicator>() {


            @Override
            public Indicator casePossiblePhoneCountIndicator(PossiblePhoneCountIndicator object) {
                // TODO Auto-generated method stub
                return object;
            }

            @Override
            public Indicator caseValidPhoneCountIndicator(ValidPhoneCountIndicator object) {
                // TODO Auto-generated method stub
                return object;
            }


            @Override
            public Indicator caseWellFormE164PhoneCountIndicator(WellFormE164PhoneCountIndicator object) {
                // TODO Auto-generated method stub
                return object;
            }

            @Override
            public Indicator caseWellFormIntePhoneCountIndicator(WellFormIntePhoneCountIndicator object) {
                // TODO Auto-generated method stub
                return object;
            }

            @Override
            public Indicator caseWellFormNationalPhoneCountIndicator(WellFormNationalPhoneCountIndicator object) {
                // TODO Auto-generated method stub
                return object;
            }
        };

        return iSwitch.doSwitch(indicator) != null;
    }
}
