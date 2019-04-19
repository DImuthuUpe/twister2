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
package edu.iu.dsc.tws.executor.comms.batch;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.batch.BKeyedGather;
import edu.iu.dsc.tws.comms.api.selectors.HashingSelector;
import edu.iu.dsc.tws.executor.comms.AbstractParallelOperation;
import edu.iu.dsc.tws.executor.comms.DefaultDestinationSelector;
import edu.iu.dsc.tws.executor.core.EdgeGenerator;
import edu.iu.dsc.tws.executor.util.Utils;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskKeySelector;
import edu.iu.dsc.tws.task.api.TaskMessage;
import edu.iu.dsc.tws.task.graph.Edge;

public class KeyedGatherBatchOperation extends AbstractParallelOperation {
  private static final Logger LOG = Logger.getLogger(KeyedGatherBatchOperation.class.getName());

  protected BKeyedGather op;

  private TaskKeySelector selector;

  public KeyedGatherBatchOperation(Config config, Communicator network, TaskPlan tPlan,
                                   Set<Integer> sources, Set<Integer> dests, EdgeGenerator e,
                                   Edge edge) {
    super(config, network, tPlan, edge.getName());
    this.edgeGenerator = e;
    this.selector = edge.getSelector();

    DestinationSelector destSelector;
    if (selector != null) {
      destSelector = new DefaultDestinationSelector(edge.getPartitioner());
    } else {
      destSelector = new HashingSelector();
    }

    boolean useDisk = false;
    Comparator keyComparator = null;
    try {
      useDisk = (Boolean) edge.getProperty("use-disk");
      keyComparator = (Comparator) edge.getProperty("key-comparator");
      LOG.info("Configuring disk based batch operation");
    } catch (Exception ex) {
      //ignore
    }

    Communicator newComm = channel.newWithConfig(edge.getProperties());
    op = new BKeyedGather(newComm, taskPlan, sources, dests,
        Utils.dataTypeToMessageType(edge.getKeyType()),
        Utils.dataTypeToMessageType(edge.getDataType()), new GatherRecvrImpl(),
        destSelector, useDisk, keyComparator);

    communicationEdge = e.generate(edge.getName());
  }

  @Override
  public boolean send(int source, IMessage message, int flags) {
    TaskMessage taskMessage = (TaskMessage) message;
    Object key = extractKey(taskMessage, selector);
    return op.gather(source, key, taskMessage.getContent(), flags);
  }

  @Override
  public boolean progress() {
    return op.progress() || op.hasPending();
  }

  private class GatherRecvrImpl implements BulkReceiver {
    @Override
    public void init(Config cfg, Set<Integer> expectedIds) {
    }

    @Override
    public boolean receive(int target, Iterator<Object> it) {
      TaskMessage msg = new TaskMessage<>(it,
          edgeGenerator.getStringMapping(communicationEdge), target);
      BlockingQueue<IMessage> messages = outMessages.get(target);
      if (messages != null) {
        return messages.offer(msg);
      } else {
        throw new RuntimeException("Un-expected message for target: " + target);
      }
    }

    @Override
    public boolean sync(int target, byte[] message) {
      return syncs.get(target).sync(edge, message);
    }
  }

  @Override
  public void finish(int source) {
    op.finish(source);
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
