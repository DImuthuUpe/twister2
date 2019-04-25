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

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.batch.BGather;
import edu.iu.dsc.tws.executor.comms.AbstractParallelOperation;
import edu.iu.dsc.tws.executor.core.EdgeGenerator;
import edu.iu.dsc.tws.executor.util.Utils;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskMessage;
import edu.iu.dsc.tws.task.graph.Edge;

public class GatherBatchOperation extends AbstractParallelOperation {
  private static final Logger LOG = Logger.getLogger(GatherBatchOperation.class.getName());
  private BGather op;

  public GatherBatchOperation(Config config, Communicator network, TaskPlan tPlan,
                              Set<Integer> srcs, Set<Integer> dests, EdgeGenerator e,
                              Edge edge) {
    super(config, network, tPlan, edge.getName());
    this.edgeGenerator = e;
    communicationEdge = e.generate(edge.getName());

    if (dests.size() > 1) {
      throw new RuntimeException("Gather can only have one target: " + dests);
    }
    Object shuffleProp = edge.getProperty("shuffle");
    boolean shuffle = false;
    if (shuffleProp instanceof Boolean && (Boolean) shuffleProp) {
      shuffle = true;
    }

    Communicator newComm = channel.newWithConfig(edge.getProperties());
    op = new BGather(newComm, taskPlan, srcs, dests.iterator().next(),
        Utils.dataTypeToMessageType(edge.getDataType()),
        new FinalGatherReceiver(), shuffle);
  }

  @Override
  public boolean send(int source, IMessage message, int flags) {
    //LOG.info("Message : " + message.getContent());
    return op.gather(source, message.getContent(), flags);
  }

  @Override
  public boolean progress() {
    return op.progress() || op.hasPending();
  }

  public boolean hasPending() {
    return op.hasPending();
  }

  @Override
  public void finish(int source) {
    op.finish(source);
  }

  private class FinalGatherReceiver implements BulkReceiver {
    // lets keep track of the messages
    // for each task we need to keep track of incoming messages
    @Override
    public void init(Config cfg, Set<Integer> expectedIds) {
    }

    @Override
    public boolean receive(int target, Iterator<Object> it) {
      // add the object to the map
      TaskMessage msg = new TaskMessage<>(it,
          edgeGenerator.getStringMapping(communicationEdge), target);
      return outMessages.get(target).offer(msg);
    }

    @Override
    public boolean sync(int target, byte[] message) {
      return syncs.get(target).sync(edge, message);
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

  @Override
  public boolean isComplete() {
    return !op.hasPending();
  }
}
