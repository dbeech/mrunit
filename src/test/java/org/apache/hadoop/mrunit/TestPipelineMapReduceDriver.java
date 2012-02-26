/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.mrunit;

import static org.apache.hadoop.mrunit.testutil.ExtendedAssert.assertListEquals;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.LongSumReducer;
import org.apache.hadoop.mrunit.types.Pair;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class TestPipelineMapReduceDriver {

  private static final int FOO_IN_A = 42;
  private static final int FOO_IN_B = 10;
  private static final int BAR_IN   = 12;
  private static final int FOO_OUT  = 52;
  private static final int BAR_OUT  = 12;

  @Test
  public void testFullyEmpty() throws IOException {
    // If no mappers or reducers are configured, then it should
    // just return its inputs. If there are no inputs, this
    // should be an empty list of outputs.
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    List<Pair<Text, Text>> out = driver.run();
    assertEquals("Expected empty output list", out.size(), 0);
  }

  @Test
  public void testEmptyPipeline() throws IOException {
    // If no mappers or reducers are configured, then it should
    // just return its inputs.
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.addInput(new Text("foo"), new Text("bar"));
    List<Pair<Text, Text>> out = driver.run();

    List<Pair<Text, Text>> expected = new ArrayList<Pair<Text, Text>>();
    expected.add(new Pair<Text, Text>(new Text("foo"), new Text("bar")));
    assertListEquals(expected, out);
  }

  @Test
  public void testEmptyPipelineWithRunTest() {
    // Like testEmptyPipeline, but call runTest.
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.withInput(new Text("foo"), new Text("bar"))
          .withOutput(new Text("foo"), new Text("bar"))
          .runTest();
  }


  @Test
  public void testSingleIdentity() {
    // Test that an identity mapper and identity reducer work
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.withMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>())
          .withInput(new Text("foo"), new Text("bar"))
          .withOutput(new Text("foo"), new Text("bar"))
          .runTest();
  }

  @Test
  public void testMultipleIdentities() {
    // Test that a pipeline of identity mapper and reducers work
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.withMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>())
          .withMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>())
          .withMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>())
          .withInput(new Text("foo"), new Text("bar"))
          .withOutput(new Text("foo"), new Text("bar"))
          .runTest();
  }

  @Test
  public void testSumAtEnd() {
    PipelineMapReduceDriver<Text, LongWritable, Text, LongWritable> driver = new PipelineMapReduceDriver<Text, LongWritable, Text, LongWritable>();
    driver.withMapReduce(new IdentityMapper<Text, LongWritable>(), new IdentityReducer<Text, LongWritable>())
          .withMapReduce(new IdentityMapper<Text, LongWritable>(), new IdentityReducer<Text, LongWritable>())
          .withMapReduce(new IdentityMapper<Text, LongWritable>(), new LongSumReducer<Text>())
          .withInput(new Text("foo"), new LongWritable(FOO_IN_A))
          .withInput(new Text("bar"), new LongWritable(BAR_IN))
          .withInput(new Text("foo"), new LongWritable(FOO_IN_B))
          .withOutput(new Text("bar"), new LongWritable(BAR_OUT))
          .withOutput(new Text("foo"), new LongWritable(FOO_OUT))
          .runTest();
  }

  @Test
  public void testSumInMiddle() {
    PipelineMapReduceDriver<Text, LongWritable, Text, LongWritable> driver = new PipelineMapReduceDriver<Text, LongWritable, Text, LongWritable>();
    driver.withMapReduce(new IdentityMapper<Text, LongWritable>(), new IdentityReducer<Text, LongWritable>())
          .withMapReduce(new IdentityMapper<Text, LongWritable>(), new LongSumReducer<Text>())
          .withMapReduce(new IdentityMapper<Text, LongWritable>(), new IdentityReducer<Text, LongWritable>())
          .withInput(new Text("foo"), new LongWritable(FOO_IN_A))
          .withInput(new Text("bar"), new LongWritable(BAR_IN))
          .withInput(new Text("foo"), new LongWritable(FOO_IN_B))
          .withOutput(new Text("bar"), new LongWritable(BAR_OUT))
          .withOutput(new Text("foo"), new LongWritable(FOO_OUT))
          .runTest();
  }

  @Test
  public void testNoMapper() {
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.addMapReduce(null, new IdentityReducer<Text, Text>());
    driver.addInput(new Text("a"), new Text("b"));
    try {
      driver.runTest();
      fail();
    } catch (IllegalStateException e) {
      assertEquals("No Mapper class was provided for stage 1 of 1", e.getMessage());
    }
  }

  @Test
  public void testNoReducer() {
    PipelineMapReduceDriver<Text, Text, Text, Text> driver = new PipelineMapReduceDriver<Text, Text, Text, Text>();
    driver.addMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>());
    driver.addMapReduce(new IdentityMapper<Text, Text>(), null);
    driver.addMapReduce(new IdentityMapper<Text, Text>(), new IdentityReducer<Text, Text>());
    driver.addInput(new Text("a"), new Text("b"));
    try {
      driver.runTest();
      fail();
    } catch (IllegalStateException e) {
      assertEquals("No Reducer class was provided for stage 2 of 3", e.getMessage());
    }
  }
}

