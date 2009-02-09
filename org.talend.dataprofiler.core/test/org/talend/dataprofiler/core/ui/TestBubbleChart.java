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

package org.talend.dataprofiler.core.ui;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.talend.cwm.exception.TalendException;
import org.talend.dataprofiler.core.ui.editor.preview.TopChartFactory;
import org.talend.dataquality.indicators.columnset.ColumnSetMultiValueIndicator;
import org.talend.dq.analysis.TestMultiColAnalysisCreation;

/**
 * DOC scorreia class global comment. Detailled comment
 */
public class TestBubbleChart {

    /**
     * DOC scorreia Comment method "main".
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            TestMultiColAnalysisCreation myTest = new TestMultiColAnalysisCreation();
            final ColumnSetMultiValueIndicator indic = myTest.run();
            JFreeChart chart = TopChartFactory.createBubbleChart(indic, indic.getNumericColumns().get(0));

            ChartFrame frame = new ChartFrame("TEST", chart);
            frame.pack();
            frame.setVisible(true);
        } catch (TalendException e) {
            e.printStackTrace();
        }

    }

}
