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

package org.openrefine.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class RowTests {

    @Test
    public void emptyRow() {
        Row row = new Row(Collections.emptyList());
        Assert.assertTrue(row.isEmpty());
    }

    @Test
    public void notEmptyRow() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        Assert.assertFalse(row.isEmpty());
    }

    @Test
    public void saveRow() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        TestUtils.isSerializedTo(
                row,
                "{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}",
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void saveIndexedRow() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        Tuple2<Long, Row> tuple = new Tuple2<Long, Row>(1234L, row);
        TestUtils.isSerializedTo(
                tuple,
                "{\"_1\":1234,\"_2\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void readIndexedRow() throws JsonParseException, JsonMappingException, IOException {
        TypeReference<Tuple2<Long, Row>> typeRef = new TypeReference<Tuple2<Long, Row>>() {
        };
        Tuple2<Long, Row> tuple = ParsingUtilities.mapper.readValue(
                "{\"_1\":1234,\"_2\":{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}}",
                typeRef);
        Assert.assertEquals((long) tuple._1, 1234L);
    }

    @Test
    public void serializeRowTest() throws Exception {

        String reconJson = "{\"id\":1533649346002675326,"
                + "\"judgmentHistoryEntry\":1530278634724,"
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"j\":\"matched\","
                + "\"m\":{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]},"
                + "\"c\":[{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]}],"
                + "\"f\":[false,false,34,0],\"judgmentAction\":\"auto\",\"judgmentBatchSize\":1,\"matchRank\":0}";

        String json = "{\"flagged\":false,"
                + "\"starred\":false,"
                + "\"cells\":["
                + "    {\"v\":\"http://www.wikidata.org/entity/Q41522540\",\"r\":" + reconJson + "},"
                + "    {\"v\":\"0000-0002-5022-0488\"},"
                + "    null,"
                + "    {\"v\":\"\"}"
                + "]}";
        Row row = Row.load(json);
        TestUtils.isSerializedTo(row, json, ParsingUtilities.saveWriter);
    }

    @Test
    public void toStringTest() {
        Row row = new Row(Arrays.asList(
                new Cell(1, null),
                new Cell(2, null),
                new Cell(3, null),
                new Cell(4, null),
                new Cell(5, null)));
        Assert.assertEquals(row.toString(), "1,2,3,4,5,");
    }

    @Test
    public void blankCell() {
        Row row = new Row(Arrays.asList(new Cell("", null)));
        Assert.assertTrue(row.isCellBlank(0));
    }

    @Test
    public void nonBlankCell() {
        Row row = new Row(Arrays.asList(new Cell("I'm not empty", null)));
        Assert.assertFalse(row.isCellBlank(0));
    }

    @Test
    public void getFlaggedField() {
        Row row = new Row(Arrays.asList(new Cell("hello", null)), true, false);
        Assert.assertTrue((Boolean) row.getField("flagged"));
    }

    @Test
    public void getStarredField() {
        Row row = new Row(Arrays.asList(new Cell("hello", null)), false, true);
        Assert.assertTrue((Boolean) row.getField("starred"));
    }

}
