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
package edu.iu.dsc.tws.executor.comms.streaming;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.ReduceFunction;
import edu.iu.dsc.tws.comms.api.SingularReceiver;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.selectors.HashingSelector;
import edu.iu.dsc.tws.comms.api.stream.SKeyedReduce;
import edu.iu.dsc.tws.executor.comms.AbstractParallelOperation;
import edu.iu.dsc.tws.executor.comms.DefaultDestinationSelector;
import edu.iu.dsc.tws.executor.core.EdgeGenerator;
import edu.iu.dsc.tws.executor.util.Utils;
import edu.iu.dsc.tws.task.api.IFunction;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskKeySelector;
import edu.iu.dsc.tws.task.api.TaskMessage;
import edu.iu.dsc.tws.task.graph.Edge;

public class KeyedReduceStreamingOperation extends AbstractParallelOperation {
  private SKeyedReduce op;

  private TaskKeySelector selector;

  public KeyedReduceStreamingOperation(Config config, Communicator network, TaskPlan tPlan,
                                       Set<Integer> sources, Set<Integer> dests, EdgeGenerator e,
                                       Edge edge) {
    super(config, network, tPlan, edge.getName());

    if (sources.size() == 0) {
      throw new RuntimeException("Sources should have more than 0 elements");
    }

    if (dests.size() == 0) {
      throw new IllegalArgumentException("Targets should have more than 0 elements");
    }
    this.selector = edge.getSelector();

    DestinationSelector destSelector;
    if (selector != null) {
      destSelector = new DefaultDestinationSelector(edge.getPartitioner());
    } else {
      destSelector = new HashingSelector();
    }

    MessageType dataType = Utils.dataTypeToMessageType(edge.getDataType());
    MessageType keyType = Utils.dataTypeToMessageType(edge.getKeyType());

    this.edgeGenerator = e;
    Communicator newComm = channel.newWithConfig(edge.getProperties());
    op = new SKeyedReduce(newComm, taskPlan, sources, dests, keyType, dataType,
        new ReduceFunctionImpl(edge.getFunction()), new SingularRecvrImpl(), destSelector);
  }

  @Override
  public boolean send(int source, IMessage message, int flags) {
    TaskMessage taskMessage = (TaskMessage) message;
    Object key = extractKey(taskMessage, selector);
    return op.reduce(source, key, taskMessage.getContent(), flags);
  }

  @Override
  public boolean progress() {
    return op.progress();
  }

  private class ReduceFunctionImpl implements ReduceFunction {
    private IFunction fn;

    ReduceFunctionImpl(IFunction fn) {
      this.fn = fn;
    }

    @Override
    public void init(Config cfg, DataFlowOperation ops,
                     Map<Integer, List<Integer>> expectedIds) {
    }

    @Override
    public Object reduce(Object t1, Object t2) {
      return fn.onMessage(t1, t2);
    }
  }

  private class SingularRecvrImpl implements SingularReceiver {
    @Override
    public void init(Config cfg, Set<Integer> expectedIds) {
    }

    @Override
    public boolean receive(int target, Object object) {
      TaskMessage msg = new TaskMessage<>(object,
          edgeGenerator.getStringMapping(communicationEdge), target);
      BlockingQueue<IMessage> messages = outMessages.get(target);
      if (messages != null) {
        if (messages.offer(msg)) {
          return true;
        }
      }
      return true;
    }
  }

  @Override
  public void close() {
    op.close();
  }

  @Override
  public void reset() {
    op.refresh();
  }
}
