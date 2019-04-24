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
package edu.iu.dsc.tws.examples.task.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Submitter;
import edu.iu.dsc.tws.api.job.Twister2Job;
import edu.iu.dsc.tws.api.task.Collector;
import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.task.Receptor;
import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.api.task.TaskWorker;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.data.api.DataType;
import edu.iu.dsc.tws.dataset.DataObject;
import edu.iu.dsc.tws.dataset.DataObjectImpl;
import edu.iu.dsc.tws.dataset.DataPartition;
import edu.iu.dsc.tws.dataset.impl.EntityPartition;
import edu.iu.dsc.tws.executor.api.ExecutionPlan;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;
import edu.iu.dsc.tws.rsched.core.SchedulerContext;
import edu.iu.dsc.tws.task.api.BaseSink;
import edu.iu.dsc.tws.task.api.BaseSource;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.task.graph.OperationMode;

public class IterativeJob extends TaskWorker {
  private static final Logger LOG = Logger.getLogger(IterativeJob.class.getName());

  @Override
  public void execute() {
    LOG.log(Level.INFO, "Task worker starting: " + workerId);

    IterativeSourceTask g = new IterativeSourceTask();
    PartitionTask r = new PartitionTask();

    TaskGraphBuilder graphBuilder = TaskGraphBuilder.newBuilder(config);
    graphBuilder.addSource("source", g, 4);
    ComputeConnection computeConnection = graphBuilder.addSink("sink", r, 4);
    computeConnection.partition("source", "partition", DataType.OBJECT);
    graphBuilder.setMode(OperationMode.BATCH);

    DataFlowTaskGraph graph = graphBuilder.build();
    ExecutionPlan plan = taskExecutor.plan(graph);
    for (int i = 0; i < 10; i++) {
      LOG.info("Starting iteration: " + i);
      taskExecutor.addInput(graph, plan, "source", "input", new DataObjectImpl<>(config));

      // this is a blocking call
      taskExecutor.itrExecute(graph, plan);
      DataObject<Object> dataSet = taskExecutor.getOutput(graph, plan, "sink");
      DataPartition<Object>[] values = dataSet.getPartitions();
    }
    taskExecutor.waitFor(graph, plan);
  }

  private static class IterativeSourceTask extends BaseSource implements Receptor {
    private static final long serialVersionUID = -254264120110286748L;

    private DataObjectImpl<Object> input;

    private int count = 0;

    @Override
    public void execute() {
      if (count == 999) {
        if (context.writeEnd("partition", "Hello")) {
          count++;
        }
      } else if (count < 999) {
        if (context.write("partition", "Hello")) {
          count++;
        }
      }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void add(String name, DataObject<?> data) {
      input = (DataObjectImpl<Object>) data;
    }

    @Override
    public void refresh() {
      count = 0;
    }
  }

  private static class PartitionTask extends BaseSink implements Collector {
    private static final long serialVersionUID = -5190777711234234L;

    private List<String> list = new ArrayList<>();

    private int count;

    @Override
    public boolean execute(IMessage message) {
      if (message.getContent() instanceof Iterator) {
        while (((Iterator) message.getContent()).hasNext()) {
          Object ret = ((Iterator) message.getContent()).next();
          count++;
          list.add(ret.toString());
        }
      }
      LOG.info("RECEIVE Count: " + count);
      return true;
    }

    @Override
    public void refresh() {
      count = 0;
    }

    @Override
    public DataPartition<Object> get() {
      return new EntityPartition<>(context.taskIndex(), list);
    }
  }

  public static void main(String[] args) {
    LOG.log(Level.INFO, "Iterative job");
    // first load the configurations from command line and config files
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    // build JobConfig
    HashMap<String, Object> configurations = new HashMap<>();
    configurations.put(SchedulerContext.THREADS_PER_WORKER, 8);

    // build JobConfig
    JobConfig jobConfig = new JobConfig();
    jobConfig.putAll(configurations);

    Twister2Job.Twister2JobBuilder jobBuilder = Twister2Job.newBuilder();
    jobBuilder.setJobName("iterative-job");
    jobBuilder.setWorkerClass(IterativeJob.class.getName());
    jobBuilder.addComputeResource(4, 1024, 4);
    jobBuilder.setConfig(jobConfig);

    // now submit the job
    Twister2Submitter.submitJob(jobBuilder.build(), config);
  }
}
