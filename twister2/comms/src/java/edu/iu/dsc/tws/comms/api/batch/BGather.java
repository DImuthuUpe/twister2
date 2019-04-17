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
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.MessageTypes;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.MToOneTree;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.comms.dfw.io.gather.DGatherBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.gather.GatherBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.gather.GatherBatchPartialReceiver;

/**
 * Batch Gather Operation
 */
public class BGather {
  /**
   * The actual operation
   */
  private MToOneTree gather;

  /**
   * The data type
   */
  private MessageType dataType;

  /**
   * Construct a Streaming Gather operation
   *
   * @param comm the communicator
   * @param plan task plan
   * @param sources source tasks
   * @param target target tasks
   * @param rcvr receiver
   * @param dataType data type
   * @param shuffle weather to use disks
   */
  public BGather(Communicator comm, TaskPlan plan,
                 Set<Integer> sources, int target,
                 MessageType dataType,
                 BulkReceiver rcvr, boolean shuffle) {
    MessageReceiver finalRcvr;
    if (!shuffle) {
      finalRcvr = new GatherBatchFinalReceiver(rcvr);
    } else {
      finalRcvr = new DGatherBatchFinalReceiver(rcvr, comm.getPersistentDirectory());
    }
    this.dataType = dataType;
    this.gather = new MToOneTree(comm.getChannel(), sources, target,
        finalRcvr, new GatherBatchPartialReceiver(target),
        0, 0, true, MessageTypes.INTEGER, dataType);
    this.gather.init(comm.getConfig(), dataType, plan, comm.nextEdge());
  }

  /**
   * Send a message to be gathered
   *
   * @param source source
   * @param message message
   * @param flags message flag
   * @return true if the message is accepted
   */
  public boolean gather(int source, Object message, int flags) {
    Tuple tuple = new Tuple(source, message, MessageTypes.INTEGER, dataType);
    return gather.send(source, tuple, flags);
  }

  /**
   * Weather we have messages pending
   *
   * @return true if there are messages pending
   */
  public boolean hasPending() {
    return !gather.isComplete();
  }

  /**
   * Indicate the end of the communication
   *
   * @param source the source that is ending
   */
  public void finish(int source) {
    gather.finish(source);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
  public boolean progress() {
    return gather.progress();
  }

  public void close() {
    // deregister from the channel
    gather.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void refresh() {
    gather.clean();
  }
}
