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
package edu.iu.dsc.tws.comms.api.stream;

import java.util.Set;

import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.SingularReceiver;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.MToNSimple;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionStreamingFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionStreamingPartialReceiver;

/**
 * Streaming Keyed Partition Operation
 */
public class SKeyedPartition {
  /**
   * The actual operation
   */
  private MToNSimple partition;

  /**
   * Destination selector
   */
  private DestinationSelector destinationSelector;

  /**
   * Construct a Streaming Key based partition operation
   *
   * @param comm the communicator
   * @param plan task plan
   * @param sources source tasks
   * @param targets target tasks
   * @param rcvr receiver
   * @param dataType data type
   */
  public SKeyedPartition(Communicator comm, TaskPlan plan,
                         Set<Integer> sources, Set<Integer> targets,
                         MessageType keyType, MessageType dataType, SingularReceiver rcvr,
                         DestinationSelector destSelector) {
    this.destinationSelector = destSelector;
    this.partition = new MToNSimple(comm.getChannel(), sources, targets,
        new PartitionStreamingFinalReceiver(rcvr), new PartitionStreamingPartialReceiver(),
        dataType, keyType);

    this.partition.init(comm.getConfig(), dataType, plan, comm.nextEdge());
    this.destinationSelector.prepare(comm, partition.getSources(), partition.getTargets());
  }

  /**
   * Send a message to be partitioned based on the key
   *
   * @param src source
   * @param key key
   * @param message message
   * @param flags message flag
   * @return true if the message is accepted
   */
  public boolean partition(int src, Object key, Object message, int flags) {
    int dest = destinationSelector.next(src, key, message);

    boolean send = partition.send(src, new Tuple<>(key, message, partition.getKeyType(),
        partition.getDataType()), flags, dest);
    if (send) {
      destinationSelector.commit(src, dest);
    }
    return send;
  }

  /**
   * Indicate the end of the communication
   *
   * @param src the source that is ending
   */
  public void finish(int src) {
    //partition.finish(src);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
  public boolean progress() {
    return partition.progress();
  }

  /**
   * Weather we have messages pending
   *
   * @return true if there are messages pending
   */
  public boolean hasPending() {
    return !partition.isComplete();
  }

  public void close() {
    // deregister from the channel
    partition.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void refresh() {
    // deregister from the channel
    partition.clean();
  }
}
