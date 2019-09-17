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
package edu.iu.dsc.tws.comms.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.comms.CommunicationContext;
import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.api.config.Config;

public class InvertedBinaryTreeRouter {
  private static final Logger LOG = Logger.getLogger(InvertedBinaryTreeRouter.class.getName());

  private Map<Integer, List<Integer>> receiveTasks;
  private Set<Integer> receiveExecutors;
  private Map<Integer, Set<Integer>> sendExternalTasksPartial;
  private Map<Integer, Set<Integer>> sendExternalTasks;
  private Map<Integer, Set<Integer>> sendInternalTasks;
  private int mainTask;
  private boolean mainTaskLast;
  private Map<Integer, Integer> destinationIdentifiers;
  private int executor;
  private Set<Integer> receiveSources = new HashSet<>();

  /**
   * Initialize the data structure
   *
   * @param cfg configuration
   * @param plan task plan
   * @param root root id
   * @param dests destinations
   */
  public InvertedBinaryTreeRouter(Config cfg, LogicalPlan plan,
                                  int root, Set<Integer> dests, int index) {
    int interNodeDegree = CommunicationContext.interNodeDegree(cfg, 2);
    int intraNodeDegree = CommunicationContext.intraNodeDegree(cfg, 2);
    this.executor = plan.getThisWorker();
    this.mainTaskLast = false;
    // lets build the tree
    BinaryTree tree = new BinaryTree(interNodeDegree, intraNodeDegree, plan, root, dests);
    Node treeRoot = tree.buildInterGroupTree(index);

    Set<Integer> thisExecutorTasks = plan.getLogicalIdsOfWorker(plan.getThisWorker());
    /*
      Tasks belonging to this operation and in the same executor
    */
    Set<Integer> thisExecutorTasksOfOperation = new HashSet<>();
    for (int t : thisExecutorTasks) {
      if (dests.contains(t) || root == t) {
        thisExecutorTasksOfOperation.add(t);
      }
    }
    LOG.fine(String.format("%d Executor Tasks: %s", plan.getThisWorker(),
        thisExecutorTasksOfOperation.toString()));
    this.destinationIdentifiers = new HashMap<>();
    // construct the map of receiving ids
    this.receiveTasks = new HashMap<Integer, List<Integer>>();

    // now lets construct the downstream tasks
    sendExternalTasksPartial = new HashMap<>();
    sendInternalTasks = new HashMap<>();
    sendExternalTasks = new HashMap<>();

    // now lets construct the receive tasks tasks
    receiveExecutors = new HashSet<>();
    for (int t : thisExecutorTasksOfOperation) {

      Node search = BinaryTree.search(treeRoot, t);
      // okay this is the main task of this executor
      if (search != null) {
        mainTask = search.getTaskId();
        LOG.fine(String.format("%d main task: %d", plan.getThisWorker(), mainTask));
        // this is the only task that receives messages
        for (int k : search.getRemoteChildrenIds()) {
          receiveExecutors.add(plan.getWorkerForForLogicalId(k));
        }
        List<Integer> recv = new ArrayList<>(search.getAllChildrenIds());
        receiveTasks.put(t, new ArrayList<>(recv));
        receiveSources.addAll(recv);

        // this task is connected to others and they send the message to this task
        List<Integer> directChildren = search.getDirectChildren();
        for (int child : directChildren) {
          Set<Integer> sendTasks = new HashSet<>();
          sendTasks.add(t);
          sendInternalTasks.put(child, sendTasks);
          destinationIdentifiers.put(child, t);
        }

        // now lets calculate the external send tasks of the main task
        Node parent = search.getParent();
        if (parent != null) {
          Set<Integer> sendTasks = new HashSet<>();
          sendTasks.add(parent.getTaskId());
          sendExternalTasksPartial.put(t, sendTasks);
          destinationIdentifiers.put(t, parent.getTaskId());
        } else {
          mainTaskLast = true;
        }
      } else {
        LOG.fine(String.format("%d doesn't have a node in tree: %d", plan.getThisWorker(), t));
      }
    }
  }

  public Set<Integer> receivingExecutors() {
    return receiveExecutors;
  }

  public Set<Integer> getReceiveSources() {
    return receiveSources;
  }

  public Map<Integer, List<Integer>> receiveExpectedTaskIds() {
    return receiveTasks;
  }

  public boolean isLastReceiver() {
    // check weather this
    return mainTaskLast;
  }

  public Map<Integer, Set<Integer>> getInternalSendTasks(int source) {
    return sendInternalTasks;
  }

  public Map<Integer, Set<Integer>> getExternalSendTasks(int source) {
    return sendExternalTasks;
  }

  public Map<Integer, Set<Integer>> getExternalSendTasksForPartial(int source) {
    return sendExternalTasksPartial;
  }

  public int mainTaskOfExecutor(int ex, int path) {
    return mainTask;
  }

  public int destinationIdentifier(int source, int path) {
    Object o = destinationIdentifiers.get(source);
    if (o != null) {
      return (int) o;
    } else {
      throw new RuntimeException(String.format("%d Unexpected source - "
              + "%s requesting destination %s", executor, source, destinationIdentifiers));
    }
  }

  public Set<Integer> sendQueueIds() {
    Set<Integer> allSends = new HashSet<>();
    allSends.addAll(sendExternalTasks.keySet());
    allSends.addAll(sendInternalTasks.keySet());
    allSends.addAll(sendExternalTasksPartial.keySet());
    allSends.add(mainTask * -1 - 1);
    return allSends;
  }

  public Map<Integer, Integer> getDestinationIdentifiers() {
    return destinationIdentifiers;
  }
}
