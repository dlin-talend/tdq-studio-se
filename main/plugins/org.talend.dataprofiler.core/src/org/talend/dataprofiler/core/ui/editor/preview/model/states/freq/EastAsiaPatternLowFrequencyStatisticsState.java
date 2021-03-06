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
package org.talend.dataprofiler.core.ui.editor.preview.model.states.freq;

import java.util.List;

import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;

/**
 * DOC talend class global comment. Detailled comment
 */
public class EastAsiaPatternLowFrequencyStatisticsState extends PatternLowFrequencyStatisticsState {

    /**
     * DOC talend EastAsiaPatternLowFrequencyStatisticsState constructor comment.
     * 
     * @param units
     */
    public EastAsiaPatternLowFrequencyStatisticsState(List<IndicatorUnit> units) {
        super(units);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.preview.model.states.freq.PatternLowFrequencyStatisticsState#getTitle()
     */
    @Override
    protected String getTitle() {
        return DefaultMessagesImpl.getString("FrequencyTypeStates.EastAsiaPatternLowFreqyebctStatistics"); //$NON-NLS-1$
    }

}
