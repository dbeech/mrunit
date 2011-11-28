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

package org.apache.hadoop.mrunit.mapreduce;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.junit.Test;

import com.google.common.collect.Lists;


public class TestReducerInputValueResuse extends TestCase {

  private class TestReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
    public LongWritable outputValue = new LongWritable();
    protected boolean instanceCheckOccured = false;
    protected boolean instanceCheckFailed = false;
    public void reduce(Text key, Iterable<LongWritable> vals, Context context)
        throws IOException, InterruptedException {
      long sum = 0;
      LongWritable inputValue = null;
      for(LongWritable val : vals) {
        if(inputValue != null) {
          instanceCheckOccured = true;
          if(inputValue != val) {
            instanceCheckFailed = true;
          }
        }
        if(inputValue == null) {
          inputValue = val;
        }
        sum += val.get();
      }
      outputValue.set(sum);
      context.write(key, outputValue);
    }
  }

  @Test
  public void testReduce() throws IOException {
    TestReducer reducer = new TestReducer();
    ReduceDriver<Text, LongWritable, Text, LongWritable> driver = 
        new ReduceDriver<Text, LongWritable, Text, LongWritable>();
    driver.setReducer(reducer);
    List<LongWritable> values = Lists.newArrayList();
    values.add(new LongWritable(1));
    values.add(new LongWritable(1));
    values.add(new LongWritable(1));
    values.add(new LongWritable(1));
    driver.withInput(new Text("foo"), values);
    driver.withOutput(new Text("foo"), new LongWritable(4));
    driver.runTest();
    assertTrue(reducer.instanceCheckOccured);
    assertFalse(reducer.instanceCheckFailed);
  }

}

