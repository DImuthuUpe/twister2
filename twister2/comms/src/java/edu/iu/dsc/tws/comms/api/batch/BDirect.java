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

import java.util.List;

import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.OneToOne;
import edu.iu.dsc.tws.comms.dfw.io.direct.DirectBatchFinalReceiver;

public class BDirect {
  /**
   * The actual operation
   */
  private OneToOne direct;

  /**
   * Construct a Batch AllReduce operation
   *
   * @param comm the communicator
   * @param plan task plan
   * @param sources source tasks
   * @param targets target tasks
   * @param rcvr receiver
   * @param dataType data type
   */
  public BDirect(Communicator comm, TaskPlan plan,
                 List<Integer> sources, List<Integer> targets,
                 BulkReceiver rcvr, MessageType dataType) {
    if (sources.size() == 0) {
      throw new IllegalArgumentException("The sources cannot be empty");
    }

    if (targets.size() == 0) {
      throw new IllegalArgumentException("The destination cannot be empty");
    }

    int middleTask = comm.nextId();
    int firstSource = sources.iterator().next();
    plan.addChannelToExecutor(plan.getExecutorForChannel(firstSource), middleTask);

    direct = new OneToOne(comm.getChannel(), sources, targets,
        new DirectBatchFinalReceiver(rcvr), comm.getConfig(), dataType, plan, comm.nextEdge());
  }

  /**
   * Send a message to be reduced
   *
   * @param src source
   * @param message message
   * @param flags message flag
   * @return true if the message is accepted
   */
  public boolean direct(int src, Object message, int flags) {
    return direct.send(src, message, flags);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
  public boolean progress() {
    return direct.progress();
  }

  /**
   * Weather we have messages pending
   * @return true if there are messages pending
   */
  public boolean hasPending() {
    return !direct.isComplete();
  }

  /**
   * Indicate the end of the communication
   * @param source the source that is ending
   */
  public void finish(int source) {
    direct.finish(source);
  }

  public void close() {
    // deregister from the channel
    direct.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void refresh() {
    direct.clean();
  }
}
