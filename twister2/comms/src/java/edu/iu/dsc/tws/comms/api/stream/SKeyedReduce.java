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

import edu.iu.dsc.tws.comms.api.CommunicationContext;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.ReduceFunction;
import edu.iu.dsc.tws.comms.api.SingularReceiver;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.MToNRing;
import edu.iu.dsc.tws.comms.dfw.MToNSimple;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.comms.dfw.io.reduce.keyed.KReduceBatchPartialReceiver;
import edu.iu.dsc.tws.comms.dfw.io.reduce.keyed.KReduceStreamingFinalReceiver;

/**
 * Streaming Keyed Partition Operation
 */
public class SKeyedReduce {
  /**
   * The actual operation
   */
  private DataFlowOperation keyedReduce;

  /**
   * Destination selector
   */
  private DestinationSelector destinationSelector;

  /**
   * Key type
   */
  private MessageType keyType;

  /**
   * Data type
   */
  private MessageType dataType;

  /**
   * Construct a Streaming Key based partition operation
   *
   * @param comm the communicator
   * @param plan task plan
   * @param sources source tasks
   * @param targets target tasks
   * @param dType data type
   * @param kType key type
   * @param fnc reduce function
   * @param rcvr receiver
   * @param destSelector destination selector
   */
  public SKeyedReduce(Communicator comm, TaskPlan plan,
                      Set<Integer> sources, Set<Integer> targets, MessageType kType,
                      MessageType dType, ReduceFunction fnc, SingularReceiver rcvr,
                      DestinationSelector destSelector) {
    this.keyType = kType;
    this.dataType = dType;

    if (CommunicationContext.TWISTER2_PARTITION_ALGO_SIMPLE.equals(
        CommunicationContext.partitionStreamAlgorithm(comm.getConfig()))) {
      this.keyedReduce = new MToNSimple(comm.getConfig(), comm.getChannel(),
          plan, sources, targets,
          new KReduceStreamingFinalReceiver(fnc, rcvr, 100),
          new KReduceBatchPartialReceiver(0, fnc), dataType, dataType,
          keyType, keyType, comm.nextEdge());
    } else if (CommunicationContext.TWISTER2_PARTITION_ALGO_RING.equals(
        CommunicationContext.partitionStreamAlgorithm(comm.getConfig()))) {
      this.keyedReduce = new MToNRing(comm.getConfig(), comm.getChannel(),
          plan, sources, targets, new KReduceStreamingFinalReceiver(fnc, rcvr, 100),
          new KReduceBatchPartialReceiver(0, fnc),
          dataType, dataType, keyType, keyType, comm.nextEdge());
    }
    this.destinationSelector = destSelector;
    this.destinationSelector.prepare(comm, sources, targets);
  }

  /**
   * Send a message to be reduced
   *
   * @param src source
   * @param key key
   * @param message message
   * @param flags message flag
   * @return true if the message is accepted
   */
  public boolean reduce(int src, Object key, Object message, int flags) {
    int dest = destinationSelector.next(src, key, message);
    return keyedReduce.send(src, new Tuple(key, message, keyType, dataType), flags, dest);
  }

  /**
   * Weather we have messages pending
   * @return true if there are messages pending
   */
  public boolean hasPending() {
    return !keyedReduce.isComplete();
  }

  /**
   * Indicate the end of the communication
   * @param src the source that is ending
   */
  public void finish(int src) {
    keyedReduce.finish(src);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
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
  public void refresh() {
    keyedReduce.clean();
  }
}
