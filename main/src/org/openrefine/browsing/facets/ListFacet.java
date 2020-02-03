/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.ProjectManager;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.FilteredRecords;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.browsing.filters.ExpressionEqualRowFilter;
import org.openrefine.browsing.util.ExpressionNominalValueGrouper;
import org.openrefine.browsing.util.StringValuesFacetState;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListFacet implements Facet {
    public static final String ERR_TOO_MANY_CHOICES = "Too many choices";
    
    /**
     * Wrapper to respect the serialization format
     */
    public static class DecoratedValueWrapper {
        @JsonProperty("v")
        public final DecoratedValue value;
        @JsonCreator
        public DecoratedValueWrapper(
                @JsonProperty("v") DecoratedValue value) {
            this.value = value;
        }
    }
    
    /*
     * Configuration
     */
    public static class ListFacetConfig implements FacetConfig {
        @JsonProperty("name")
        public String     name;
        @JsonProperty("expression")
        public String     expression;
        @JsonProperty("columnName")
        public String     columnName;
        @JsonProperty("invert")
        public boolean    invert;
        
        // If true, then facet won't show the blank and error choices
        @JsonProperty("omitBlank")
        public boolean omitBlank;
        @JsonProperty("omitError")
        public boolean omitError;
        
        @JsonIgnore
        public List<DecoratedValue> selection = new LinkedList<>();
        @JsonProperty("selectBlank")
        public boolean selectBlank;
        @JsonProperty("selectError")
        public boolean selectError;

        @JsonProperty("selection")
        public List<DecoratedValueWrapper> getWrappedSelection() {
            return selection.stream()
                    .map(e -> new DecoratedValueWrapper(e))
                    .collect(Collectors.toList());
        }
        
        @JsonProperty("selection")
        public void setSelection(List<DecoratedValueWrapper> wrapped) {
            selection = wrapped.stream()
                    .map(e -> e.value)
                    .collect(Collectors.toList());
        }
        
        @Override
        public Facet apply(ColumnModel columnModel) {
            return new ListFacet(this, columnModel);
        }

        @Override
        public String getJsonType() {
            return "core/list";
        }
    }
    
    /**
     * Wrapper class for choice counts and selection status for blank and error
     */
    public static class OtherChoice {
        @JsonProperty("s")
        boolean selected;
        @JsonProperty("c")
        int count;
        public OtherChoice(
                @JsonProperty("s") boolean selected,
                @JsonProperty("c") int count) {
            this.selected = selected;
            this.count = count;
        }
    }
    
    final ListFacetConfig _config;
    final ColumnModel _columnModel;
    
    /*
     * Derived configuration
     */
    protected int        _cellIndex;
    protected Evaluable  _eval;
    protected String     _errorMessage;
    
    /*
     * Computed results
     */
    protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
    protected int _blankCount;
    protected int _errorCount;
    
    public ListFacet(ListFacetConfig config, ColumnModel model) {
    	_config = config;
    	_columnModel = model;
    	
        if (_config.columnName.length() > 0) {
            _cellIndex = _columnModel.getColumnIndexByName(_config.columnName);
            if (_cellIndex == -1) {
            	_errorMessage = "No column named " + _config.columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_config.expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
    }
    
    @JsonProperty("name")
    public String getName() {
        return _config.name;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _config.columnName;
    }
    
    @JsonProperty("expression")
    public String getExpression() {
        return _config.expression;
    }
    
    @JsonProperty("invert")
    public boolean getInvert() {
        return _config.invert;
    }
    
    @JsonProperty("error")
    @JsonInclude(Include.NON_NULL)
    public String getError() {
        if (_errorMessage == null && _choices.size() > getLimit()) {
            return ERR_TOO_MANY_CHOICES;
        }
        return _errorMessage;
    }
    
    @JsonProperty("choiceCount")
    @JsonInclude(Include.NON_NULL)
    public Integer getChoiceCount() {
        if (_errorMessage == null && _choices.size() > getLimit()) {
            return _choices.size();
        }
        return null;
    }
    
    @JsonProperty("choices")
    @JsonInclude(Include.NON_NULL)
    public List<NominalFacetChoice> getChoices() {
        if (getError() == null) {
            return _choices;
        }
        return null;
    }
    
    @JsonProperty("blankChoice")
    @JsonInclude(Include.NON_NULL)
    public OtherChoice getBlankChoice() {
        if (getError() == null && !_config.omitBlank && (_config.selectBlank || _blankCount > 0)) {
            return new OtherChoice(_config.selectBlank, _blankCount);
        }
        return null;
    }
    
    @JsonProperty("errorChoice")
    @JsonInclude(Include.NON_NULL)
    public OtherChoice getErrorChoice() {
        if (getError() == null && !_config.omitError && (_config.selectError || _errorCount > 0)) {
            return new OtherChoice(_config.selectError, _errorCount);
        }
        return null;
    }
    
    protected int getLimit() {
        Object v = ProjectManager.singleton.getPreferenceStore().get("ui.browsing.listFacet.limit");
        if (v != null) {
            if (v instanceof Number) {
                return ((Number) v).intValue();
            } else {
                try {
                    int n = Integer.parseInt(v.toString());
                    return n;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return 2000;
    }

    @Override
    public RowFilter getRowFilter() {
        return 
            _eval == null || 
            _errorMessage != null ||
            (_config.selection.size() == 0 && !_config.selectBlank && !_config.selectError) ? 
                null :
                new ExpressionEqualRowFilter(
                    _eval, 
                    _config.columnName,
                    _cellIndex, 
                    createMatches(), 
                    _config.selectBlank, 
                    _config.selectError,
                    _config.invert);
    }
    
    @Override
    public RecordFilter getRecordFilter() {
        RowFilter rowFilter = getRowFilter();
        return rowFilter == null ? null :
            (_config.invert ?
                    new AllRowsRecordFilter(rowFilter) :
                        new AnyRowRecordFilter(rowFilter));
    }
    
    protected void postProcessGrouper(ExpressionNominalValueGrouper grouper) {
        _choices.clear();
        _choices.addAll(grouper.choices.values());
        
        for (DecoratedValue decoratedValue : _config.selection) {
            String valueString = decoratedValue.value.toString();
            
            if (grouper.choices.containsKey(valueString)) {
                grouper.choices.get(valueString).selected = true;
            } else {
                /*
                 *  A selected choice can have zero count if it is selected together
                 *  with other choices, and some other facets' constraints eliminate
                 *  all rows projected to this choice altogether. For example, if you
                 *  select both "car" and "bicycle" in the "type of vehicle" facet, and
                 *  then constrain the "wheels" facet to more than 2, then the "bicycle"
                 *  choice now has zero count even if it's still selected. The grouper 
                 *  won't be able to detect the "bicycle" choice, so we need to inject
                 *  that choice into the choice list ourselves.
                 */
                NominalFacetChoice choice = new NominalFacetChoice(decoratedValue);
                choice.count = 0;
                choice.selected = true;
                _choices.add(choice);
            }
        }
        
        _blankCount = grouper.blankCount;
        _errorCount = grouper.errorCount;
    }
    
    protected Object[] createMatches() {
        Object[] a = new Object[_config.selection.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = _config.selection.get(i).value;
        }
        return a;
    }
    
	@Override
	public FacetState getInitialFacetState() {
		return new StringValuesFacetState(this, _columnModel, _eval, _cellIndex);
	}
}
