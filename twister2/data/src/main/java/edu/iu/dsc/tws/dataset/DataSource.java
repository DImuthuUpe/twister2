//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.dataset;

import java.io.IOException;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.data.api.InputPartitioner;
import edu.iu.dsc.tws.data.fs.io.InputSplit;
import edu.iu.dsc.tws.data.fs.io.InputSplitAssigner;

/**
 * A distributed data source
 *
 * @param <T> type of the data
 * @param <O> type of InputSplit used for splitting the data
 */
public class DataSource<T, O extends InputSplit<T>> extends DataObjectImpl<T> {
  private static final Logger LOG = Logger.getLogger(DataSource.class.getName());

  private InputPartitioner<T, O> input;

  private O[] splits;

  private Config config;

  public DataSource(Config cfg, InputPartitioner<T, O> input, int numSplits) {
    super(cfg);
    this.config = cfg;
    this.input = input;
    this.input.configure(cfg);
    try {
      this.splits = this.input.createInputSplits(numSplits);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to create the input splits because, the %s", e.getMessage()));
    }
  }

  public InputSplit<T> getNextSplit(int id) {
    InputSplitAssigner assigner = input.getInputSplitAssigner(splits);
    InputSplit<T> split = assigner.getNextInputSplit("localhost", id);
    if (split != null) {
      try {
        split.open(config);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to open the input split because, the %s", e.getMessage()));
      }
      return split;
    } else {
      return null;
    }
  }
}
