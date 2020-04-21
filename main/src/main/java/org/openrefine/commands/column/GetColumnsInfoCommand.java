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

package org.openrefine.commands.column;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;

import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.NumericBinIndex;
import org.openrefine.browsing.util.NumericBinRowIndex;
import org.openrefine.commands.Command;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;

public class GetColumnsInfoCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Project project = getProject(request);

            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(response.getWriter());
            List<ColumnMetadata> columns = project.getColumnModel().getColumns();
            writer.writeStartArray();
            for (int i = 0; i != columns.size(); i++) {
                writer.writeStartObject();
                write(project, columns.get(i), i, writer);
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }

    private NumericBinIndex getBinIndex(Project project, ColumnMetadata column, int cellIndex) {
        String expression = "value";
        String key = "numeric-bin:" + expression;
        Evaluable eval = null;
        try {
            eval = MetaParser.parse(expression);
        } catch (ParsingException e) {
            // this should never happen
        }
        NumericBinIndex index = null;
        // TODO migrate this
        if (index == null) {
            index = new NumericBinRowIndex(project, new ExpressionBasedRowEvaluable(column.getName(), cellIndex, eval));
        }
        return index;
    }

    private void write(Project project, ColumnMetadata column, int index, JsonGenerator writer) throws IOException {
        NumericBinIndex columnIndex = getBinIndex(project, column, index);
        if (columnIndex != null) {
            writer.writeStringField("name", column.getName());
            boolean is_numeric = columnIndex.isNumeric();
            writer.writeBooleanField("is_numeric", is_numeric);
            writer.writeNumberField("numeric_row_count", columnIndex.getNumericRowCount());
            writer.writeNumberField("non_numeric_row_count", columnIndex.getNonNumericRowCount());
            writer.writeNumberField("error_row_count", columnIndex.getErrorRowCount());
            writer.writeNumberField("blank_row_count", columnIndex.getBlankRowCount());
            if (is_numeric) {
                writer.writeNumberField("min", columnIndex.getMin());
                writer.writeNumberField("max", columnIndex.getMax());
                writer.writeNumberField("step", columnIndex.getStep());
            }
        } else {
            writer.writeStringField("error", "error finding numeric information on the '" + column.getName() + "' column");
        }
    }
}