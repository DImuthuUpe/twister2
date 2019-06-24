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

import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.comms.utils.TaskPlanUtils;

public class PartitionRouter {
  private static final Logger LOG = Logger.getLogger(PartitionRouter.class.getName());
  // the task plan
  private LogicalPlan logicalPlan;
  // task -> (path -> tasks)
  private Map<Integer, Set<Integer>> externalSendTasks;
  // task -> (path -> tasks)
  private Map<Integer, Set<Integer>> internalSendTasks;
  // task -> (path -> tasks)
  private Map<Integer, List<Integer>> upstream;
  private Set<Integer> receiveExecutors;

  private Map<Integer, List<Integer>> partialReceives;

  /**
   * Create a direct router
   */
  public PartitionRouter(LogicalPlan plan, Set<Integer> srscs, Set<Integer> dests) {
    this.logicalPlan = plan;

    this.externalSendTasks = new HashMap<>();
    this.internalSendTasks = new HashMap<>();
    this.partialReceives = new HashMap<>();

    Set<Integer> myTasks = logicalPlan.getChannelsOfExecutor(logicalPlan.getThisExecutor());
    for (int src : srscs) {
      if (myTasks.contains(src)) {
        for (int dest : dests) {
          // okay the destination is in the same executor
          if (myTasks.contains(dest)) {
            if (!internalSendTasks.containsKey(src)) {
              Set<Integer> set = new HashSet<>();
              set.add(dest);
              internalSendTasks.put(src, set);
            } else {
              internalSendTasks.get(src).add(dest);
            }

          } else {
            if (!externalSendTasks.containsKey(src)) {
              Set<Integer> set = new HashSet<>();
              set.add(dest);
              externalSendTasks.put(src, set);
            } else {
              externalSendTasks.get(src).add(dest);
            }

          }
        }
      }
    }

    // we are going to receive from all the sources
    this.upstream = new HashMap<>();
    List<Integer> sources = new ArrayList<>(srscs);
    for (int dest : dests) {
      if (myTasks.contains(dest)) {
        this.upstream.put(dest, sources);
      }
    }

    receiveExecutors = PartitionRouter.getExecutorsHostingTasks(plan, srscs);
    // we are not interested in our own
    receiveExecutors.remove(logicalPlan.getThisExecutor());

    List<Integer> thisSources = new ArrayList<>(
        TaskPlanUtils.getTasksOfThisWorker(logicalPlan, srscs));
    for (int dest : dests) {
      partialReceives.put(dest, thisSources);
    }
  }

  public Set<Integer> receivingExecutors() {
    return receiveExecutors;
  }

  public Map<Integer, List<Integer>> receiveExpectedTaskIds() {
    // check if this executor contains
    return upstream;
  }

  public Map<Integer, List<Integer>> partialExpectedTaskIds() {
    return partialReceives;
  }

  public Map<Integer, Set<Integer>> getInternalSendTasks() {
    // return a routing
    return internalSendTasks;
  }

  public Map<Integer, Set<Integer>> getExternalSendTasks() {
    return externalSendTasks;
  }

  public int mainTaskOfExecutor() {
    return -1;
  }

  private static Set<Integer> getExecutorsHostingTasks(LogicalPlan plan, Set<Integer> tasks) {
    Set<Integer> executors = new HashSet<>();

    Set<Integer> allExecutors = plan.getAllExecutors();
    LOG.fine(String.format("%d All executors: %s", plan.getThisExecutor(), allExecutors));
    for (int e : allExecutors) {
      Set<Integer> tasksOfExecutor = plan.getChannelsOfExecutor(e);
      LOG.fine(String.format("%d Tasks of executors: %s", plan.getThisExecutor(), tasksOfExecutor));
      for (int t : tasks) {
        if (tasksOfExecutor.contains(t)) {
          executors.add(e);
          break;
        }
      }
    }

    return executors;
  }
}
