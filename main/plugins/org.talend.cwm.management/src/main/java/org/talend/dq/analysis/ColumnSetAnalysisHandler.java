// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dq.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.cwm.dependencies.DependenciesHandler;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.management.i18n.Messages;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataquality.helpers.AnalysisHelper;
import org.talend.dataquality.helpers.IndicatorHelper;
import org.talend.dataquality.helpers.MetadataHelper;
import org.talend.dataquality.indicators.CompositeIndicator;
import org.talend.dataquality.indicators.DataminingType;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.columnset.AllMatchIndicator;
import org.talend.dataquality.indicators.columnset.SimpleStatIndicator;
import org.talend.dq.indicators.definitions.DefinitionHandler;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.foundation.softwaredeployment.DataManager;
import orgomg.cwm.objectmodel.core.Dependency;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * DOC yyi class global comment. Detailled comment
 */
public class ColumnSetAnalysisHandler extends AnalysisHandler {

    private static Logger log = Logger.getLogger(ColumnSetAnalysisHandler.class);

    /**
     * The resources that are connected to this analysis and that are potentially modified.
     */
    // private Collection<Resource> modifiedResources = new HashSet<Resource>();
    /**
     * Method "addColumnToAnalyze".
     * 
     * @param column
     * @return
     */

    public boolean addIndicator(List<ModelElement> columns, Indicator indicator) {
        for (ModelElement tdColumn : columns) {
            if (!analysis.getContext().getAnalysedElements().contains(tdColumn)) {
                analysis.getContext().getAnalysedElements().add(tdColumn);
            }
        }

        // store first level of indicators in result.
        analysis.getResults().getIndicators().add(indicator);
        initializeIndicator(indicator);

        DataManager connection = analysis.getContext().getConnection();

        if (connection == null) { // try to get one
            for (ModelElement element : columns) {
                TdColumn tdColumn = SwitchHelpers.COLUMN_SWITCH.doSwitch(element);
                log.error(Messages.getString("ColumnCorrelationAnalysisHandler.CONNNOTBEENSETINANALYSIS"));//$NON-NLS-1$
                connection = ConnectionHelper.getTdDataProvider(tdColumn);
                if (connection != null) {
                    analysis.getContext().setConnection(connection);
                    break;
                }
            }
        }
        TypedReturnCode<Dependency> rc = DependenciesHandler.getInstance().setDependencyOn(analysis, connection);

        return rc.isOk();
    }

    // MOD mzhao bug 10706,Set indicator definition.
    private void initializeIndicator(Indicator indicator) {
        if (indicator.getIndicatorDefinition() == null) {
            DefinitionHandler.getInstance().setDefaultIndicatorDefinition(indicator);
        }
        if (indicator instanceof CompositeIndicator) {
            for (Indicator child : ((CompositeIndicator) indicator).getChildIndicators()) {
                initializeIndicator(child); // recurse
            }
        }

    }

    /**
     * Method "setDatamingType".
     * 
     * @param dataminingTypeLiteral the literal expression of the datamining type used for the analysis
     * @param column a column
     */
    public void setDatamingType(String dataminingTypeLiteral, TdColumn column) {
        DataminingType type = DataminingType.get(dataminingTypeLiteral);
        MetadataHelper.setDataminingType(type, column);
        Resource resource = column.eResource();
        if (resource != null) {
            resource.setModified(true); // tell that the resource has been modified.
            // it would be better to handle modifications with EMF Commands
            // this.modifiedResources.add(resource);
        }
    }

    /**
     * DOC BZhou Comment method "getDatamingType".
     * 
     * @param column
     * @return
     */
    public DataminingType getDatamingType(TdColumn column) {

        return MetadataHelper.getDataminingType(column);
    }

    /**
     * Method "getIndicators".
     * 
     * @param column
     * @return the indicators attached to this column
     */
    public Indicator getSimpleStatIndicator() {
        Indicator indicator = null;
        EList<Indicator> allIndics = analysis.getResults().getIndicators();
        for (Indicator indic : allIndics) {
            if (indic != null && indic instanceof SimpleStatIndicator) {
                indicator = indic;
                break;
            }
        }
        return indicator;
    }

    /**
     * 
     * DOC mzhao get AllMetchIndicator.
     * 
     * @return
     */
    public Indicator getAllmatchIndicator() {
        Indicator indicator = null;
        EList<Indicator> allIndics = analysis.getResults().getIndicators();
        for (Indicator indic : allIndics) {
            if (indic != null && indic instanceof AllMatchIndicator) {
                indicator = indic;
                break;
            }
        }
        return indicator;
    }

    public Collection<Indicator> getRegexMathingIndicators(ModelElement modelElement) {
        Collection<Indicator> indics = new ArrayList<Indicator>();
        EList<Indicator> allIndics = analysis.getResults().getIndicators();
        for (Indicator indicator : allIndics) {
            if (indicator instanceof AllMatchIndicator) {
                AllMatchIndicator allMatchInd = (AllMatchIndicator) indicator;
                for (Indicator regexInd : allMatchInd.getCompositeRegexMatchingIndicators()) {
                    if (regexInd.getAnalyzedElement() != null && regexInd.getAnalyzedElement().equals(modelElement)) {
                        indics.add(regexInd);
                    }
                }
                break;
            }
        }
        return indics;
    }

    /**
     * Method "getIndicatorLeaves" returns the indicators for the given column at the leaf level.
     * 
     * @param column
     * @return the indicators attached to this column
     */
    public Collection<Indicator> getIndicatorLeaves(TdColumn column) {
        // get the leaf indicators
        Collection<Indicator> leafIndics = IndicatorHelper.getIndicatorLeaves(analysis.getResults());
        // filter only indicators for this column
        Collection<Indicator> indics = new ArrayList<Indicator>();
        for (Indicator indicator : leafIndics) {
            if (indicator.getAnalyzedElement() != null && indicator.getAnalyzedElement().equals(column)) {
                indics.add(indicator);
            }
        }
        return indics;
    }

    /**
     * Method "setStringDataFilter".
     * 
     * @param datafilterString
     * @return true when a new data filter is created, false if it is only updated
     */
    public boolean setStringDataFilter(String datafilterString) {
        return AnalysisHelper.setStringDataFilter(analysis, datafilterString);
    }

    public String getStringDataFilter() {
        return AnalysisHelper.getStringDataFilter(analysis);
    }

}
