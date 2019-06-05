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
package edu.iu.dsc.tws.executor.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.task.api.schedule.ContainerPlan;
import edu.iu.dsc.tws.task.api.schedule.TaskInstancePlan;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;

public final class TaskPlanBuilder {
  private TaskPlanBuilder() {
  }

  /**
   * Create a task plan based on the resource plan from resources and scheduled plan
   *
   * @param schedulePlan schedule plan
   * @param idGenerator global task id generator
   * @return the task plan
   */
  public static TaskPlan build(int workerID, List<JobMasterAPI.WorkerInfo> workerInfoList,
                               TaskSchedulePlan schedulePlan, TaskIdGenerator idGenerator) {
    Set<ContainerPlan> cPlanList = schedulePlan.getContainers();
    Map<Integer, Set<Integer>> containersToTasks = new HashMap<>();
    Map<Integer, Set<Integer>> groupsToTasks = new HashMap<>();

    // we need to sort to keep the order
    workerInfoList.sort(Comparator.comparingInt(JobMasterAPI.WorkerInfo::getWorkerID));

    for (ContainerPlan c : cPlanList) {
      Set<TaskInstancePlan> tSet = c.getTaskInstances();
      Set<Integer> instances = new HashSet<>();

      for (TaskInstancePlan tPlan : tSet) {
        instances.add(idGenerator.generateGlobalTaskId(tPlan.getTaskId(), tPlan.getTaskIndex()));
      }
      containersToTasks.put(c.getContainerId(), instances);
    }

    Map<String, List<JobMasterAPI.WorkerInfo>> containersPerNode = new TreeMap<>();
    for (JobMasterAPI.WorkerInfo workerInfo : workerInfoList) {
      String name = Integer.toString(workerInfo.getWorkerID());
      List<JobMasterAPI.WorkerInfo> containerList;
      if (!containersPerNode.containsKey(name)) {
        containerList = new ArrayList<>();
        containersPerNode.put(name, containerList);
      } else {
        containerList = containersPerNode.get(name);
      }
      containerList.add(workerInfo);
    }

    Map<String, Set<Integer>> nodeToTasks = new HashMap<>();

    int i = 0;
    // we take each container as an executor
    for (Map.Entry<String, List<JobMasterAPI.WorkerInfo>> entry : containersPerNode.entrySet()) {
      Set<Integer> executorsOfGroup = new HashSet<>();
      for (JobMasterAPI.WorkerInfo workerInfo : entry.getValue()) {
        executorsOfGroup.add(workerInfo.getWorkerID());
        Set<Integer> tasksInNode = nodeToTasks.computeIfAbsent(
            workerInfo.getNodeInfo().getNodeIP(),
            k -> new HashSet<>());
        tasksInNode.addAll(containersToTasks.get(workerInfo.getWorkerID()));
      }
      groupsToTasks.put(i, executorsOfGroup);
      i++;
    }

    return new TaskPlan(containersToTasks, groupsToTasks, nodeToTasks, workerID);
  }
}
