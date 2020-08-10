/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.model.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.model.recon.StandardReconConfig.ColumnDetail;
import org.openrefine.model.recon.StandardReconConfig.ReconResult;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ReconOperation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class StandardReconConfigTests extends RefineTest {

    @BeforeMethod
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon", ReconOperation.class);
        ReconConfig.registerReconConfig("core", "standard-service", StandardReconConfig.class);
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private class StandardReconConfigStub extends StandardReconConfig {

        public StandardReconConfigStub() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }

        protected Recon createReconServiceResults(String text, ArrayNode resultsList, long historyEntryID) throws IOException {
            return super.createReconServiceResults(text, resultsList, historyEntryID);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(0.5, r);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertTrue(!Double.isInfinite(r));
        Assert.assertTrue(!Double.isNaN(r));
    }

    @Test
    public void serializeStandardReconConfig() throws Exception {
        String json = " {\n" +
                "        \"mode\": \"standard-service\",\n" +
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" +
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" +
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" +
                "        \"type\": {\n" +
                "                \"id\": \"Q13442814\",\n" +
                "                \"name\": \"scientific article\"\n" +
                "        },\n" +
                "        \"autoMatch\": true,\n" +
                "        \"columnDetails\": [\n" +
                "           {\n" +
                "             \"column\": \"organization_country\",\n" +
                "             \"propertyName\": \"SPARQL: P17/P297\",\n" +
                "             \"propertyID\": \"P17/P297\"\n" +
                "           },\n" +
                "           {\n" +
                "             \"column\": \"organization_id\",\n" +
                "             \"propertyName\": \"SPARQL: P3500|P2427\",\n" +
                "             \"propertyID\": \"P3500|P2427\"\n" +
                "           }\n" +
                "        ],\n" +
                "        \"limit\": 0\n" +
                " }";
        ReconConfig config = ReconConfig.reconstruct(json);
        TestUtils.isSerializedTo(config, json, ParsingUtilities.defaultWriter);

        // the "mode" only appears once in the serialization result
        String fullJson = ParsingUtilities.mapper.writeValueAsString(config);
        assertEquals(fullJson.indexOf("\"mode\"", fullJson.indexOf("\"mode\"") + 1), -1);
    }

    @Test
    public void testReconstructNoType() throws IOException {
        String json = "{\"mode\":\"standard-service\","
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"type\":null,"
                + "\"autoMatch\":true,"
                + "\"columnDetails\":["
                + "    {\"column\":\"_ - id\","
                + "     \"property\":{\"id\":\"P3153\",\"name\":\"Crossref funder ID\"}}"
                + "],"
                + "\"limit\":0}";
        StandardReconConfig config = StandardReconConfig.reconstruct(json);
        assertNull(config.typeID);
        assertNull(config.typeName);
    }

    @Test
    public void testReconstructNoIdentifierSchemaSpaces() throws IOException {
        String json = "{\"mode\":\"standard-service\","
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"type\":null,"
                + "\"autoMatch\":true,"
                + "\"columnDetails\":[],"
                + "\"limit\":0}";
        StandardReconConfig config = StandardReconConfig.reconstruct(json);
        assertEquals(config.identifierSpace, "http://localhost/identifier");
        assertEquals(config.schemaSpace, "http://localhost/schema");
    }

    @Test
    public void formulateQueryTest() throws IOException {
        Project project = createProject(
                new String[] { "title", "director" },
                new Serializable[] {
                        "mulholland drive", "david lynch" });

        String config = " {\n" +
                "        \"mode\": \"standard-service\",\n" +
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" +
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" +
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" +
                "        \"type\": {\n" +
                "                \"id\": \"Q1234\",\n" +
                "                \"name\": \"movie\"\n" +
                "        },\n" +
                "        \"autoMatch\": true,\n" +
                "        \"columnDetails\": [\n" +
                "           {\n" +
                "             \"column\": \"director\",\n" +
                "             \"propertyName\": \"Director\",\n" +
                "             \"propertyID\": \"P123\"\n" +
                "           }\n" +
                "        ]}";
        StandardReconConfig r = StandardReconConfig.reconstruct(config);
        Row row = project.rows.get(0);
        ReconJob job = r.createJob(null, 0, row, "title", row.getCell(0));
        TestUtils.assertEqualAsJson("{"
                + "\"query\":\"mulholland drive\","
                + "\"type\":\"Q1234\","
                + "\"properties\":["
                + "     {\"pid\":\"P123\",\"v\":\"david lynch\"}"
                + "],"
                + "\"type_strict\":\"should\"}", job.toString());
    }

    /**
     * The UI format and the backend format differ for serialization (the UI never deserializes and the backend
     * serialization did not matter). TODO: change the frontend so it uses the same format.
     */
    @Test
    public void deserializeColumnDetail() throws JsonParseException, JsonMappingException, IOException {
        String uiJson = "{\"column\":\"director\","
                + "\"property\":{"
                + "   \"id\":\"P123\","
                + "   \"name\":\"Director\""
                + "}}";
        String backendJson = "{\"column\":\"director\","
                + "\"propertyID\":\"P123\","
                + "\"propertyName\":\"Director\"}";
        ColumnDetail cd = ParsingUtilities.mapper.readValue(uiJson, ColumnDetail.class);
        TestUtils.isSerializedTo(cd, backendJson, ParsingUtilities.defaultWriter);
    }

    @Test
    public void deserializeReconResult() throws JsonParseException, JsonMappingException, IOException {
        String json = "{\"score\":100.0,"
                + "\"match\":false,"
                + "\"type\":["
                + "   {\"id\":\"Q17366755\","
                + "    \"name\":\"hamlet in Alberta\"}],"
                + "\"id\":\"Q5136635\","
                + "\"name\":\"Cluny\"}";
        ReconResult rr = ParsingUtilities.mapper.readValue(json, ReconResult.class);
        assertEquals(rr.types.get(0).name, "hamlet in Alberta");
    }

    // Issue #1913
    @Test
    public void reorderReconciliationResults() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = " [\n" +
                "\n" +
                "    {\n" +
                "        \"id\": \"18951129\",\n" +
                "        \"name\": \"Varano, Camilla Battista da 1458-1524\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.1282051282051282,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"102271932\",\n" +
                "        \"name\": \"Shamsie, Kamila, 1973-....\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.23076923076923078,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"63233597\",\n" +
                "        \"name\": \"Camilla, Duchess of Cornwall, 1947-\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.14285714285714285,\n" +
                "        \"match\": false\n" +
                "    }\n" +
                "\n" +
                "]";

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertTrue(recon.candidates.get(0).score > 0.2);
        assertEquals(recon.candidates.get(0).id, "102271932");
    }

    @Test
    public void reorderReconciliationResultsStableSort() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = " [\n" +
                "\n" +
                "    {\n" +
                "        \"id\": \"18951129\",\n" +
                "        \"name\": \"Varano, Camilla Battista da 1458-1524\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.3,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"102271932\",\n" +
                "        \"name\": \"Shamsie, Kamila, 1973-....\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.23076923076923078,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"63233597\",\n" +
                "        \"name\": \"Camilla, Duchess of Cornwall, 1947-\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.3,\n" +
                "        \"match\": false\n" +
                "    }\n" +
                "\n" +
                "]";

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertEquals(recon.candidates.get(0).score, 0.3);
        assertEquals(recon.candidates.get(0).id, "18951129");
    }

    /**
     * computing the features on an empty recon should not fail
     */
    @Test
    public void testComputeFeatures() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, "my string");
        assertNotNull(recon.features);
    }

    /**
     * Should not happen, but added for extra safety
     */
    @Test
    public void testComputeFeaturesNullText() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, null);
        assertNotNull(recon.features);
    }
}
