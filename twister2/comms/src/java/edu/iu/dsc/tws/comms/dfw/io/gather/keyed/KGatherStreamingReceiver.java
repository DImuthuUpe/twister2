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
package edu.iu.dsc.tws.comms.dfw.io.gather.keyed;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.dfw.io.AggregatedObjects;
import edu.iu.dsc.tws.comms.dfw.io.KeyedReceiver;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;

/**
 * Abstract class that is extended by keyed reduce batch receivers
 */
public abstract class KGatherStreamingReceiver extends KeyedReceiver {
  private static final Logger LOG = Logger.getLogger(
      KGatherStreamingReceiver.class.getName());

  /**
   * List used to send data from the partial receiver. This is used because once we take elements
   * from the send queue we cannot put them back in if the send fails. So the send messages are
   * kept in the variable until the send method returns true.
   */
  private List<Object> sendList = new AggregatedObjects<>();

  /**
   * Flags associated with the current sendList
   */
  private int flags = 0;

  /**
   * Streaming messages are only kept until the window size is met. by default the window size
   * is 1, so all messages are forwarded as they arrive.
   */
  protected int windowSize = 1;

  /**
   * variable used to keep track of the current local window count. This value is always reset
   * to 0 after it reaches the windowSize
   */
  protected int localWindowCount;

  /**
   * checks if the queue structures used to send data is empty. If Additional data structures are
   * used this method needs to be overwritten to include them. This method overrides the default
   * implementation and also checks to make sure the sendList is also empty
   *
   * @param targetSendQueue message queue for the current target
   * @return true if all the related queues and structures are empty
   */
  @Override
  protected boolean isAllQueuesEmpty(Queue<Object> targetSendQueue) {
    return targetSendQueue.isEmpty() && sendList.isEmpty();
  }

  /**
   * saves the given message (or messages if the object is a list) into the messages data structure
   * if possible and rejects the message if the whole message cannot be added to the messages
   * data structure.
   *
   * @param target target for which the messages are to be added
   * @param object the message/messages to be added
   * @return true if the message was added or false otherwise
   */
  @SuppressWarnings("rawtypes")
  protected boolean offerMessage(int target, Object object) {
    Map<Object, Queue<Object>> messagesPerTarget = messages.get(target);
    if (messagesPerTarget.size() > keyLimit) {
      LOG.fine(String.format("Executor %d Partial cannot add any further keys needs flush ",
          executor));
      moveMessagesToSendQueue(target, messagesPerTarget);
      return false;
    } else if (localWindowCount > windowSize) {
      if (moveMessagesToSendQueue(target, messagesPerTarget)) {
        //TODO: what if the move returns false, do we still set the localWindowCount to zero?
        localWindowCount = 0;
      }
    }

    if (object instanceof List) {
      List dataList = (List) object;
      Map<Object, List<Object>> tempList = new HashMap<>();
      for (Object dataEntry : dataList) {
        Tuple tuple = (Tuple) dataEntry;
        //If any of the keys are full the method returns false because partial objects cannot be
        //added to the messages data structure
        Object key = tuple.getKey();
        if (!isFinalBatchReceiver && messagesPerTarget.containsKey(key)
            && messagesPerTarget.get(key).size() >= limitPerKey) {
          moveMessageToSendQueue(target, messagesPerTarget, tuple.getKey());
          LOG.fine(String.format("Executor %d Partial cannot add any further values for key "
              + "needs flush ", executor));
          return false;
        }
        if (tempList.containsKey(key)) {
          tempList.get(key).add(tuple.getValue());
        } else {
          tempList.put(key, new AggregatedObjects<>());
          tempList.get(key).add(tuple.getValue());

        }

      }
      boolean offerDone = true;
      for (Object key : tempList.keySet()) {
        if (messagesPerTarget.containsKey(key)) {
          List<Object> values = tempList.get(key);
          for (Object value : values) {
            offerDone &= messagesPerTarget.get(key).offer(value);
            localWindowCount++;
          }
        } else {
          ArrayDeque<Object> messagesPerKey = new ArrayDeque<>();
          List<Object> values = tempList.get(key);
          for (Object value : values) {
            offerDone &= messagesPerKey.offer(value);
            localWindowCount++;
          }
          messagesPerTarget.put(key, messagesPerKey);
        }
      }

      //If even one of the message offers failed we throw an exception since that message
      //cannot be recovered
      if (!offerDone) {
        throw new RuntimeException("Message lost during processing");
      }

    } else {
      Tuple tuple = (Tuple) object;
      if (messagesPerTarget.containsKey(tuple.getKey())) {
        if (messagesPerTarget.get(tuple.getKey()).size() < limitPerKey
            || isFinalBatchReceiver) {
          localWindowCount++;
          return messagesPerTarget.get(tuple.getKey()).offer(tuple.getValue());
        } else {
          LOG.fine(String.format("Executor %d Partial cannot add any further values for key "
              + "needs flush ", executor));
          moveMessageToSendQueue(target, messagesPerTarget, tuple.getKey());
          return false;
        }
      } else {
        ArrayDeque<Object> messagesPerKey = new ArrayDeque<>();
        messagesPerKey.add(tuple.getValue());
        messagesPerTarget.put(tuple.getKey(), messagesPerKey);
        localWindowCount++;
      }
    }
    if (localWindowCount > windowSize) {

      if (moveMessagesToSendQueue(target, messagesPerTarget)) {
        //TODO: what if the move returns false, do we still set the localWindowCount to zero?
        localWindowCount = 0;
      }
    }
    return true;
  }

  @Override
  protected boolean sendToTarget(boolean needsFurtherProgress, boolean sourcesFinished, int target,
                                 Queue<Object> targetSendQueue) {
    // We only try to send new messages if the sendList is empty. If it still has values
    // that means a previous senPartial call returned false. so we need to first send that
    // data before processing new data
    boolean needsProgress = needsFurtherProgress;
    if (sendList.isEmpty()) {
      while (!targetSendQueue.isEmpty()) {
        if (sourcesFinished && targetSendQueue.size() == 1) {
          flags = MessageFlags.LAST;
        }
        sendList.add(targetSendQueue.poll());
      }
    }

    if (!sendList.isEmpty()) {
      if (dataFlowOperation.sendPartial(representSource, sendList, flags, target)) {
        sendList = new AggregatedObjects<>();
        flags = 0;
      } else {
        needsProgress = true;
      }
    }
    return needsProgress;
  }
}
