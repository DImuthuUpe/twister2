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
package edu.iu.dsc.tws.api.comms;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information about the instances in which the communication happens.
 * This holds the mapping from the physical level communication addresses which are based on
 * the communication library to high level ids that are defined by upper layers.
 */
public class LogicalPlan {
  /**
   * Map from executor to message channel ids, we assume unique channel ids across the cluster
   */
  private Map<Integer, Set<Integer>> executorToChannels = new HashMap<Integer, Set<Integer>>();

  /**
   * channel to executor mapping for easy access
   */
  private Map<Integer, Integer> invertedExecutorToChannels = new HashMap<Integer, Integer>();

  /**
   * Executors can be grouped
   */
  private Map<Integer, Integer> executorToGroup = new HashMap<>();

  /**
   * Group to executor
   */
  private Map<Integer, Set<Integer>> groupsToExecutor = new HashMap<>();

  private Map<String, Set<Integer>> nodeToTasks;
  /**
   * The process under which we are running
   */
  private int thisExecutor;


  public LogicalPlan(Map<Integer, Set<Integer>> executorToChannels,
                     Map<Integer, Set<Integer>> groupsToExecutor,
                     Map<String, Set<Integer>> nodeToTasks,
                     int thisExecutor) {
    this.executorToChannels = executorToChannels;
    this.nodeToTasks = nodeToTasks;
    this.thisExecutor = thisExecutor;
    this.groupsToExecutor = groupsToExecutor;

    for (Map.Entry<Integer, Set<Integer>> e : executorToChannels.entrySet()) {
      for (Integer c : e.getValue()) {
        invertedExecutorToChannels.put(c, e.getKey());
      }
    }

    for (Map.Entry<Integer, Set<Integer>> e : groupsToExecutor.entrySet()) {
      for (Integer ex : e.getValue()) {
        executorToGroup.put(ex, e.getKey());
      }
    }
  }

  public int getExecutorForChannel(int channel) {
    Object ret = invertedExecutorToChannels.get(channel);
    if (ret == null) {
      return -1;
    }
    return (int) ret;
  }

  public Set<Integer> getChannelsOfExecutor(int executor) {
    return executorToChannels.getOrDefault(executor, Collections.emptySet());
  }

  public int getThisExecutor() {
    return thisExecutor;
  }

  public Set<Integer> getAllExecutors() {
    return executorToChannels.keySet();
  }

  public Set<Integer> getExecutesOfGroup(int group) {
    if (groupsToExecutor.keySet().size() == 0) {
      return new HashSet<>(executorToChannels.keySet());
    }
    return groupsToExecutor.get(group);
  }

  public int getGroupOfExecutor(int executor) {
    if (executorToGroup.containsKey(executor)) {
      return executorToGroup.get(executor);
    }
    return 0;
  }

  public void addChannelToExecutor(int executor, int channel) {
    Set<Integer> values = executorToChannels.get(executor);
    if (values == null) {
      throw new RuntimeException("Cannot add to non-existent worker: " + executor);
    }
    if (values.contains(channel)) {
      throw new RuntimeException("Cannot add existing channel: " + channel);
    }
    values.add(channel);
    invertedExecutorToChannels.put(channel, executor);
  }

  public Set<Integer> getTasksOfThisExecutor() {
    return executorToChannels.get(thisExecutor);
  }

  public Map<String, Set<Integer>> getNodeToTasks() {
    return nodeToTasks;
  }

  public int getIndexOfTaskInNode(int task) {
    Optional<Set<Integer>> tasksInThisNode = this.getNodeToTasks().values()
        .stream().filter(tasks -> tasks.contains(task)).findFirst();
    if (tasksInThisNode.isPresent()) {
      List<Integer> sortedTargets = tasksInThisNode.get().stream().sorted()
          .collect(Collectors.toList());
      return sortedTargets.indexOf(task);
    } else {
      throw new RuntimeException("Couldn't find task in any node");
    }
  }

  @Override
  public String toString() {
    return "LogicalPlan{"
        + "executorToChannels=" + executorToChannels
        + ", groupsToExecutor=" + groupsToExecutor
        + ", thisExecutor=" + thisExecutor
        + '}';
  }
}
