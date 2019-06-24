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
package edu.iu.dsc.tws.comms.batch;

import java.util.List;
import java.util.Set;

import edu.iu.dsc.tws.api.comms.BulkReceiver;
import edu.iu.dsc.tws.api.comms.CommunicationContext;
import edu.iu.dsc.tws.api.comms.Communicator;
import edu.iu.dsc.tws.api.comms.DataFlowOperation;
import edu.iu.dsc.tws.api.comms.DestinationSelector;
import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.api.comms.messaging.MessageReceiver;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.comms.dfw.MToNRing;
import edu.iu.dsc.tws.comms.dfw.MToNSimple;
import edu.iu.dsc.tws.comms.dfw.io.partition.DPartitionBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionPartialReceiver;

/**
 * Streaming Partition Operation
 */
public class BPartition {
  /**
   * The actual operation
   */
  private DataFlowOperation partition;

  /**
   * Destination selector
   */
  private DestinationSelector destinationSelector;

  /**
   * Construct a Batch partition operation
   *
   * @param comm the communicator
   * @param plan task plan
   * @param sources source tasks
   * @param targets target tasks
   * @param rcvr receiver
   * @param dataType data type
   * @param destSelector destination selector
   */
  public BPartition(Communicator comm, LogicalPlan plan,
                    Set<Integer> sources, Set<Integer> targets, MessageType dataType,
                    BulkReceiver rcvr,
                    DestinationSelector destSelector, boolean shuffle) {
    this.destinationSelector = destSelector;
    List<String> shuffleDirs = comm.getPersistentDirectories();

    MessageReceiver finalRcvr;
    if (shuffle) {
      finalRcvr = new DPartitionBatchFinalReceiver(
          rcvr, false, shuffleDirs, null, true);
    } else {
      finalRcvr = new PartitionBatchFinalReceiver(rcvr);
    }

    if (CommunicationContext.TWISTER2_PARTITION_ALGO_SIMPLE.equals(
        CommunicationContext.partitionBatchAlgorithm(comm.getConfig()))) {
      MToNSimple p = new MToNSimple(comm.getChannel(), sources, targets,
          finalRcvr, new PartitionPartialReceiver(), dataType);
      p.init(comm.getConfig(), dataType, plan, comm.nextEdge());
      this.partition = p;
    } else if (CommunicationContext.TWISTER2_PARTITION_ALGO_RING.equals(
        CommunicationContext.partitionBatchAlgorithm(comm.getConfig()))) {
      this.partition = new MToNRing(comm.getConfig(), comm.getChannel(),
          plan, sources, targets, finalRcvr, new PartitionPartialReceiver(),
          dataType, dataType, null, null, comm.nextEdge());
    }
    this.destinationSelector.prepare(comm, partition.getSources(), partition.getTargets());
  }

  /**
   * Send a message to be partitioned
   *
   * @param source source
   * @param message message
   * @param flags message flag
   * @return true if the message is accepted
   */
  public boolean partition(int source, Object message, int flags) {
    int dest = destinationSelector.next(source, message);

    boolean send = partition.send(source, message, flags, dest);
    if (send) {
      destinationSelector.commit(source, dest);
    }
    return send;
  }

  /**
   * Weather we have messages pending
   *
   * @return true if there are messages pending
   */
  public boolean hasPending() {
    return !partition.isComplete();
  }

  /**
   * Indicate the end of the communication
   *
   * @param source the source that is ending
   */
  public void finish(int source) {
    partition.finish(source);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
  public boolean progress() {
    return partition.progress();
  }

  public void close() {
    partition.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void reset() {
    partition.reset();
  }
}
