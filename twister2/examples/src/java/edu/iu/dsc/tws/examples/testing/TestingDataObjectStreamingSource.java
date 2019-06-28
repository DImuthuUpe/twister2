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
package edu.iu.dsc.tws.examples.testing;

import java.util.logging.Logger;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.data.Path;
import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.task.TaskContext;
import edu.iu.dsc.tws.api.task.executor.ExecutorContext;
import edu.iu.dsc.tws.api.task.modifiers.Collector;
import edu.iu.dsc.tws.api.task.nodes.BaseSource;
import edu.iu.dsc.tws.data.api.formatters.LocalTextInputPartitioner;
import edu.iu.dsc.tws.dataset.DataSource;
import edu.iu.dsc.tws.executor.core.ExecutionRuntime;

public class TestingDataObjectStreamingSource extends BaseSource implements Collector {

  private static final Logger LOG = Logger.getLogger(
      TestingDataObjectStreamingSource.class.getName());

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

  public TestingDataObjectStreamingSource(String edgename, String dataDirectory) {
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
   */
  public String getEdgeName() {
    return edgeName;
  }

  /**
   * Setter property to set the edge name
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
    int i = 0;
    while (i < 100) {
      context.write(getEdgeName(), "StreamingSource");
      ++i;
    }
    //context.end(getEdgeName());
  }

  @Override
  public void prepare(Config cfg, TaskContext context) {
    super.prepare(cfg, context);
    ExecutionRuntime runtime = (ExecutionRuntime) cfg.get(
        ExecutorContext.TWISTER2_RUNTIME_OBJECT);
    this.source = runtime.createInput(cfg, context, new LocalTextInputPartitioner(
        new Path(getDataDirectory()), context.getParallelism(), config));
  }

  @Override
  public DataPartition<?> get() {
    return null;
  }
}
