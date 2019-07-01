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
package edu.iu.dsc.tws.examples.task.streaming;

import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.comms.Op;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.api.task.nodes.BaseSource;
import edu.iu.dsc.tws.api.task.nodes.ISink;
import edu.iu.dsc.tws.examples.task.BenchTaskWorker;
import edu.iu.dsc.tws.task.impl.TaskGraphBuilder;
import edu.iu.dsc.tws.task.typed.streaming.SKeyedReduceCompute;

/**
 * todo keyedreduce for streaming is not applicable. This will be removed,
 * or used only with windowing
 */
public class STKeyedReduceExample extends BenchTaskWorker {

  private static final Logger LOG = Logger.getLogger(STKeyedReduceExample.class.getName());

  @Override
  public TaskGraphBuilder buildTaskGraph() {
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);
    Op operation = Op.SUM;
    MessageType keyType = MessageTypes.INTEGER;
    MessageType dataType = MessageTypes.INTEGER_ARRAY;
    String edge = "edge";
    BaseSource g = new SourceTask(edge, true);
    ISink r = new KeyedReduceSinkTask();
    taskGraphBuilder.addSource(SOURCE, g, sourceParallelism);
    computeConnection = taskGraphBuilder.addSink(SINK, r, sinkParallelism);
    computeConnection.keyedReduce(SOURCE)
        .viaEdge(edge)
        .withOperation(operation, dataType)
        .withKeyType(keyType);
    return taskGraphBuilder;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected static class KeyedReduceSinkTask
      extends SKeyedReduceCompute<Integer, int[]> implements ISink {

    private static final long serialVersionUID = -254264903510284798L;

    @Override
    public boolean keyedReduce(Tuple<Integer, int[]> content) {
      return true;
    }
  }
}
