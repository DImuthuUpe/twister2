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
package edu.iu.dsc.tws.comms.api.batch;

import java.util.Set;

import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.ReduceFunction;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.MToNSimple;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionPartialReceiver;
import edu.iu.dsc.tws.comms.dfw.io.reduce.keyed.KReduceBatchFinalReceiver;

/**
 * Example class for Batch keyed reduce. The reduce destination for each data point will be
 * based on the key value related to that data point.
 */
public class BKeyedReduce {
  private DataFlowOperation keyedReduce;

  private DestinationSelector destinationSelector;

  private MessageType keyType;

  private MessageType dataType;

  public BKeyedReduce(Communicator comm, TaskPlan plan,
                      Set<Integer> sources, Set<Integer> destinations, ReduceFunction fnc,
                      BulkReceiver rcvr, MessageType kType, MessageType dType,
                      DestinationSelector destSelector) {
    this.keyType = kType;
    this.dataType = dType;
    MessageReceiver partialReceiver = new PartitionPartialReceiver();

    this.keyedReduce = new MToNSimple(comm.getConfig(), comm.getChannel(),
        plan, sources, destinations,
        new KReduceBatchFinalReceiver(fnc, rcvr),
        partialReceiver, dataType, dataType,
        keyType, keyType, comm.nextEdge());
    this.destinationSelector = destSelector;
    this.destinationSelector.prepare(comm, sources, destinations);
  }

  public boolean reduce(int src, Object key, Object data, int flags) {
    int dest = destinationSelector.next(src, key, data);
    return keyedReduce.send(src, new Tuple<>(key, data, keyType, dataType), flags, dest);
  }

  public boolean hasPending() {
    return !keyedReduce.isComplete();
  }

  public void finish(int src) {
    keyedReduce.finish(src);
  }

  public boolean progress() {
    return keyedReduce.progress();
  }

  public void close() {
    // deregister from the channel
    keyedReduce.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void reset() {
    keyedReduce.reset();
  }
}
