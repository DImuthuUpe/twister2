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
package edu.iu.dsc.tws.tsched.taskscheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.task.api.schedule.ContainerPlan;
import edu.iu.dsc.tws.task.api.schedule.TaskInstancePlan;
import edu.iu.dsc.tws.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.tsched.spi.common.TaskSchedulerContext;
import edu.iu.dsc.tws.tsched.spi.scheduler.TaskSchedulerException;
import edu.iu.dsc.tws.tsched.spi.scheduler.WorkerPlan;
import edu.iu.dsc.tws.tsched.spi.taskschedule.ITaskScheduler;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;

/**
 * This class invokes the appropriate task schedulers based on the 'streaming' or 'batch' task types
 * and scheduling modes 'roundrobin', 'firstfit', and 'datalocality'.
 */
public class TaskScheduler implements ITaskScheduler {

  private static final Logger LOG = Logger.getLogger(TaskScheduler.class.getName());

  private Config config;

  private DataFlowTaskGraph dataFlowTaskGraph;

  private WorkerPlan workerPlan;

  private String schedulingType;

  private int workerId;

  @Override
  public void initialize(Config cfg) {
    this.config = cfg;
  }

  @Override
  public void initialize(Config cfg, int workerid) {
    this.config = cfg;
    this.workerId = workerid;
  }

  /**
   * This is the base method for the task scheduler to invoke the appropriate task schedulers
   * either "batch" or "streaming" based on the task type.
   */
  @Override
  public TaskSchedulePlan schedule(DataFlowTaskGraph graph, WorkerPlan plan) {

    this.dataFlowTaskGraph = graph;
    this.workerPlan = plan;

    TaskSchedulePlan taskSchedulePlan = null;

    if ("STREAMING".equals(graph.getOperationMode().toString())) {
      taskSchedulePlan = scheduleStreamingTask();
    } else if ("BATCH".equals(graph.getOperationMode().toString())) {
      taskSchedulePlan = scheduleBatchTask();
    }
    return taskSchedulePlan;
  }

  /**
   * This method invokes the appropriate streaming task schedulers based on the scheduling mode
   * specified in the task configuration by the user or else from the default configuration value.
   */
  private TaskSchedulePlan scheduleStreamingTask() {

    if (config.getStringValue("SchedulingMode") != null) {
      this.schedulingType = config.getStringValue("SchedulingMode");
    } else {
      this.schedulingType = TaskSchedulerContext.streamingTaskSchedulingMode(config);
    }
    LOG.fine("Task Scheduling Type:" + schedulingType + "(" + "streaming task" + ")");
    return generateTaskSchedulePlan(TaskSchedulerContext.streamingTaskSchedulingClass(config));
  }

  /**
   * This method invokes the appropriate batch task schedulers based on the scheduling mode
   * specified in the task configuration by the user or else from the default configuration value.
   *
   * @return Task Schedule Plan
   */
  private TaskSchedulePlan scheduleBatchTask() {

    if (config.getStringValue("SchedulingMode") != null) {
      this.schedulingType = config.getStringValue("SchedulingMode");
    } else {
      this.schedulingType = TaskSchedulerContext.batchTaskSchedulingMode(config);
    }
    LOG.fine("Task Scheduling Type:" + schedulingType + "(" + "batch task" + ")");
    return generateTaskSchedulePlan(TaskSchedulerContext.batchTaskSchedulingClass(config));
  }

  private TaskSchedulePlan generateTaskSchedulePlan(String className) {
    Class<?> taskSchedulerClass;
    Method method;
    TaskSchedulePlan taskSchedulePlan;
    try {
      taskSchedulerClass = ClassLoader.getSystemClassLoader().loadClass(className);
      Object newInstance = taskSchedulerClass.newInstance();
      LOG.info("%%%% Instantiated Task Scheduler Class:%%%%" + taskSchedulerClass);
      method = taskSchedulerClass.getMethod("initialize", new Class<?>[]{Config.class});
      method.invoke(newInstance, config);
      method = taskSchedulerClass.getMethod("schedule",
          new Class<?>[]{DataFlowTaskGraph.class, WorkerPlan.class});
      taskSchedulePlan = (TaskSchedulePlan) method.invoke(newInstance, dataFlowTaskGraph,
          workerPlan);
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException
        | InstantiationException | ClassNotFoundException | TaskSchedulerException e) {
      throw new RuntimeException("Task Schedule Plan Not Able to Create:" + e.getMessage());
    }

    if (taskSchedulePlan != null) {
      Map<Integer, ContainerPlan> containersMap
          = taskSchedulePlan.getContainersMap();
      for (Map.Entry<Integer, ContainerPlan> entry : containersMap.entrySet()) {
        Integer integer = entry.getKey();
        ContainerPlan containerPlan = entry.getValue();
        Set<TaskInstancePlan> containerPlanTaskInstances
            = containerPlan.getTaskInstances();
        LOG.fine("Task Details for Container Id:" + integer);
        for (TaskInstancePlan ip : containerPlanTaskInstances) {
          LOG.fine("Task Id:" + ip.getTaskId()
              + "\tTask Index" + ip.getTaskIndex()
              + "\tTask Name:" + ip.getTaskName());
        }
      }
    }
    return taskSchedulePlan;
  }
}
