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
package edu.iu.dsc.tws.examples.task.dataparallel;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.compute.TaskContext;
import edu.iu.dsc.tws.api.compute.executor.ExecutorContext;
import edu.iu.dsc.tws.api.compute.nodes.BaseSource;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.data.FileSystem;
import edu.iu.dsc.tws.api.data.Path;
import edu.iu.dsc.tws.data.api.formatters.LocalTextInputPartitioner;
import edu.iu.dsc.tws.data.api.formatters.SharedTextInputPartitioner;
import edu.iu.dsc.tws.data.api.out.TextOutputWriter;
import edu.iu.dsc.tws.data.fs.io.InputSplit;
import edu.iu.dsc.tws.dataset.DataSink;
import edu.iu.dsc.tws.dataset.DataSource;
import edu.iu.dsc.tws.examples.comms.Constants;
import edu.iu.dsc.tws.executor.core.ExecutionRuntime;

public class DataParallelTask extends BaseSource {
  private static final Logger LOG = Logger.getLogger(DataParallelTask.class.getName());

  private static final long serialVersionUID = -1L;

  private DataSource<String, ?> source;

  private DataSink<String> sink;

  @Override
  public void execute() {
    InputSplit<String> inputSplit = source.getNextSplit(context.taskIndex());
    int splitCount = 0;
    int totalCount = 0;
    while (inputSplit != null) {
      try {
        int count = 0;
        while (!inputSplit.reachedEnd()) {
          String value = inputSplit.nextRecord(null);
          if (value != null) {
            LOG.fine("We read value: " + value);
            sink.add(context.taskIndex(), value);
            count += 1;
            totalCount += 1;
          }
        }
        splitCount += 1;
        inputSplit = source.getNextSplit(context.taskIndex());
        LOG.info("Finished: " + context.taskIndex() + " count: " + count
            + " split: " + splitCount + " total count: " + totalCount);
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Failed to read the input", e);
      }
    }
    sink.persist();
  }

  @Override
  public void prepare(Config cfg, TaskContext context) {
    super.prepare(cfg, context);

    String directory = cfg.getStringValue(Constants.ARGS_INPUT_DIRECTORY);
    ExecutionRuntime runtime = (ExecutionRuntime) config.get(
        ExecutorContext.TWISTER2_RUNTIME_OBJECT);
    String outDir = cfg.getStringValue(Constants.ARGS_OUTPUT_DIRECTORY);

    boolean shared = cfg.getBooleanValue(Constants.ARGS_SHARED_FILE_SYSTEM);
    if (!shared) {
      this.source = runtime.createInput(cfg, context,
          new LocalTextInputPartitioner(new Path(directory), context.getParallelism()));
    } else {
      this.source = runtime.createInput(cfg, context,
          new SharedTextInputPartitioner(new Path(directory)));
    }
    this.sink = new DataSink<String>(cfg,
        new TextOutputWriter(FileSystem.WriteMode.OVERWRITE, new Path(outDir)));
  }
}
