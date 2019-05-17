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
package edu.iu.dsc.tws.examples.batch.mds;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.dataobjects.DataObjectSource;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.data.api.formatters.BinaryInputPartitioner;
import edu.iu.dsc.tws.data.api.splits.FileInputSplit;
import edu.iu.dsc.tws.data.fs.Path;
import edu.iu.dsc.tws.data.fs.io.InputSplit;
import edu.iu.dsc.tws.dataset.DataSource;
import edu.iu.dsc.tws.executor.core.ExecutionRuntime;
import edu.iu.dsc.tws.executor.core.ExecutorContext;
import edu.iu.dsc.tws.task.api.BaseSource;
import edu.iu.dsc.tws.task.api.TaskContext;

public class MDSDataObjectSource extends BaseSource {

  private static final Logger LOG = Logger.getLogger(DataObjectSource.class.getName());

  private static final long serialVersionUID = -1L;

  /**
   * DataSource to partition the datapoints
   */
  private DataSource<?, ?> source;

  private BinaryInputPartitioner inputPartitioner;
  private FileInputSplit[] splits;
  private InputSplit currentSplit;

  /**
   * Edge name to write the partitoned datapoints
   */
  private String edgeName;
  private String dataDirectory;

  public MDSDataObjectSource(String edgename, String dataDirectory) {
    this.edgeName = edgename;
    this.dataDirectory = dataDirectory;
  }

  public String getDataDirectory() {
    return dataDirectory;
  }

  public void setDataDirectory(String dataDirectory) {
    this.dataDirectory = dataDirectory;
  }

  /**
   * Getter property to set the edge name
   * @return
   */
  public String getEdgeName() {
    return edgeName;
  }

  /**
   * Setter property to set the edge name
   * @param edgeName
   */
  public void setEdgeName(String edgeName) {
    this.edgeName = edgeName;
  }

  /**
   * This method get the partitioned datapoints using the task index and write those values using
   * the respective edge name.
   */
  @Override
  public void execute() {
    InputSplit<?> inputSplit = source.getNextSplit(context.taskIndex());
    while (inputSplit != null) {
      try {
        while (!inputSplit.reachedEnd()) {
          Object value = inputSplit.nextRecord(null);
          if (value != null) {
            context.write(getEdgeName(), value);
          }
        }
        inputSplit = source.getNextSplit(context.taskIndex());
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Failed to read the input", e);
      }
    }
    context.end(getEdgeName());
  }

  @Override
  public void prepare(Config cfg, TaskContext context) {
    super.prepare(cfg, context);
    ExecutionRuntime runtime = (ExecutionRuntime) cfg.get(ExecutorContext.TWISTER2_RUNTIME_OBJECT);
    this.source = runtime.createInput(cfg, context, new BinaryInputPartitioner(
        new Path(getDataDirectory()), 2000 * Short.BYTES));
  }
}
