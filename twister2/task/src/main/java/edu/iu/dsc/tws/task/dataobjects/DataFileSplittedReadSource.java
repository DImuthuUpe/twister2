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
package edu.iu.dsc.tws.task.dataobjects;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.data.Path;
import edu.iu.dsc.tws.api.task.TaskContext;
import edu.iu.dsc.tws.api.task.executor.ExecutorContext;
import edu.iu.dsc.tws.api.task.nodes.BaseSource;
import edu.iu.dsc.tws.data.api.formatters.LocalTextInputPartitioner;
import edu.iu.dsc.tws.data.fs.io.InputSplit;
import edu.iu.dsc.tws.dataset.DataSource;
import edu.iu.dsc.tws.executor.core.ExecutionRuntime;

/**
 * This class is responsible for partition the datapoints which is based on the task parallelism
 * value. This class may use either the "LocalFixedInputPartitioner" or "LocalTextInputPartitioner"
 * to partition the datapoints. Finally, write the partitioned datapoints into their respective
 * edges.
 */
public class DataFileSplittedReadSource<T> extends BaseSource {

  private static final Logger LOG = Logger.getLogger(DataFileSplittedReadSource.class.getName());

  private static final long serialVersionUID = -1L;

  /**
   * DataSource to partition the datapoints
   */
  private DataSource<?, ?> source;

  /**
   * Edge name to write the partitoned datapoints
   */
  private String edgeName;

  private String dataDirectory;

  public String getDataDirectory() {
    return dataDirectory;
  }

  public void setDataDirectory(String dataDirectory) {
    this.dataDirectory = dataDirectory;
  }

  public DataFileSplittedReadSource(String edgename) {
    this.edgeName = edgename;
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
    LOG.fine("Context Task Index:" + context.taskIndex() + "\t" + getEdgeName());
    InputSplit<?> inputSplit = source.getNextSplit(context.taskIndex());
    int totalCount = 0;
    while (inputSplit != null) {
      try {
        int count = 0;
        while (!inputSplit.reachedEnd()) {
          Object value = inputSplit.nextRecord(null);
          if (value != null) {
            context.write(getEdgeName(), value);
            count += 1;
            totalCount += 1;
          }
        }
        inputSplit = source.getNextSplit(context.taskIndex());
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Failed to read the input", e);
      }
    }
    context.end(getEdgeName());
  }

  /**
   * Retrieve the data input directory using the DataObjectConstants and the config file.
   * @param cfg
   * @param context
   */
  @Override
  public void prepare(Config cfg, TaskContext context) {
    super.prepare(cfg, context);
    ExecutionRuntime runtime = (ExecutionRuntime) cfg.get(ExecutorContext.TWISTER2_RUNTIME_OBJECT);
    this.source = runtime.createInput(cfg, context, new LocalTextInputPartitioner(
        new Path(getDataDirectory()), context.getParallelism(), config));
  }
}
