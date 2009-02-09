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
package org.talend.dataprofiler.core.ui.editor.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.ui.forms.editor.FormEditor;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.cwm.relational.TdCatalog;
import org.talend.cwm.relational.TdSchema;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.schema.CatalogIndicator;
import org.talend.dataquality.indicators.schema.SchemaIndicator;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.objectmodel.core.Package;

/**
 * DOC rli class global comment. Detailled comment
 */
public class SchemaAnalysisMasterDetailsPage extends AbstractFilterMetadataPage {

    private List<SchemaIndicator> schemaIndicatorList = new ArrayList<SchemaIndicator>();

    private List<TdSchema> schemas = new ArrayList<TdSchema>();

    public SchemaAnalysisMasterDetailsPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    @Override
    protected void fillDataProvider() {
        EList<ModelElement> analysedElements = this.analysis.getContext().getAnalysedElements();
        tdDataProvider = null;
        if (analysedElements.size() > 0) {
            ModelElement modelElement = analysedElements.get(0);
            tdDataProvider = DataProviderHelper.getTdDataProvider((Package) modelElement);
        }
    }

    @Override
    protected List<CatalogIndicator> getCatalogIndicators() {
        return Collections.emptyList();
    }

    @Override
    protected List<TdCatalog> getCatalogs() {
        return Collections.emptyList();
    }

    @Override
    protected List<SchemaIndicator> getSchemaIndicators() {
        EList<Indicator> indicators = analysis.getResults().getIndicators();
        schemaIndicatorList.clear();
        for (Indicator indicator : indicators) {
            schemaIndicatorList.add((SchemaIndicator) indicator);
        }
        return schemaIndicatorList;
    }
}
