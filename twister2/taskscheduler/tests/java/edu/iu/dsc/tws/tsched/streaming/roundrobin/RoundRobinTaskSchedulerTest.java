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
package edu.iu.dsc.tws.tsched.streaming.roundrobin;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.config.Context;
import edu.iu.dsc.tws.comms.api.MessageTypes;
import edu.iu.dsc.tws.comms.api.Op;
import edu.iu.dsc.tws.task.api.schedule.ContainerPlan;
import edu.iu.dsc.tws.task.api.schedule.TaskInstancePlan;
import edu.iu.dsc.tws.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.task.graph.OperationMode;
import edu.iu.dsc.tws.tsched.spi.scheduler.Worker;
import edu.iu.dsc.tws.tsched.spi.scheduler.WorkerPlan;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;
import edu.iu.dsc.tws.tsched.utils.TaskSchedulerClassTest;

public class RoundRobinTaskSchedulerTest {

  private static final Logger LOG = Logger.getLogger(RoundRobinTaskSchedulerTest.class.getName());

  @Test
  public void testUniqueSchedules() {
    int parallel = 256;
    DataFlowTaskGraph graph = createGraph(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(parallel);

    for (int i = 0; i < 1; i++) {
      TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);
      TaskSchedulePlan plan2 = scheduler.schedule(graph, workerPlan);

      Assert.assertEquals(plan1.getContainers().size(), plan2.getContainers().size());

      Map<Integer, ContainerPlan> map2 = plan2.getContainersMap();
      for (ContainerPlan containerPlan : plan1.getContainers()) {
        ContainerPlan p2 = map2.get(containerPlan.getContainerId());
        Assert.assertTrue(containerEquals(containerPlan, p2));
      }
    }
  }

  @Test
  public void testUniqueSchedules2() {
    int parallel = 256;
    DataFlowTaskGraph graph = createGraph(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(parallel);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    WorkerPlan workerPlan2 = createWorkPlan2(parallel);
    for (int i = 0; i < 1000; i++) {
      TaskSchedulePlan plan2 = scheduler.schedule(graph, workerPlan2);

      Assert.assertEquals(plan1.getContainers().size(), plan2.getContainers().size());

      Map<Integer, ContainerPlan> map2 = plan2.getContainersMap();
      for (ContainerPlan containerPlan : plan1.getContainers()) {
        ContainerPlan p2 = map2.get(containerPlan.getContainerId());

        Assert.assertTrue(containerEquals(containerPlan, p2));
      }
    }
  }

  @Test
  public void testUniqueSchedules3() {
    int parallel = 16;
    int workers = 2;
    DataFlowTaskGraph graph = createGraphWithGraphConstraints(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(workers);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    Map<Integer, ContainerPlan> containersMap = plan1.getContainersMap();
    for (Map.Entry<Integer, ContainerPlan> entry : containersMap.entrySet()) {

      ContainerPlan containerPlan = entry.getValue();
      Set<TaskInstancePlan> containerPlanTaskInstances = containerPlan.getTaskInstances();

      Assert.assertEquals(containerPlanTaskInstances.size(), Integer.parseInt(
          graph.getGraphConstraints().get(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
    }
  }

  @Test
  public void testUniqueSchedules4() {
    int parallel = 16;
    int workers = 2;
    DataFlowTaskGraph graph = createGraphWithComputeTaskAndConstraints(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(workers);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    Map<Integer, ContainerPlan> containersMap = plan1.getContainersMap();
    for (Map.Entry<Integer, ContainerPlan> entry : containersMap.entrySet()) {

      ContainerPlan containerPlan = entry.getValue();
      Set<TaskInstancePlan> containerPlanTaskInstances = containerPlan.getTaskInstances();

      Assert.assertEquals(containerPlanTaskInstances.size(), Integer.parseInt(
          graph.getGraphConstraints().get(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
    }
  }

  private boolean containerEquals(ContainerPlan p1,
                                  ContainerPlan p2) {
    if (p1.getContainerId() != p2.getContainerId()) {
      return false;
    }

    if (p1.getTaskInstances().size() != p2.getTaskInstances().size()) {
      return false;
    }

    for (TaskInstancePlan instancePlan : p1.getTaskInstances()) {
      if (!p2.getTaskInstances().contains(instancePlan)) {
        return false;
      }
    }
    return true;
  }

  private WorkerPlan createWorkPlan(int workers) {
    WorkerPlan plan = new WorkerPlan();
    for (int i = 0; i < workers; i++) {
      plan.addWorker(new Worker(i));
    }
    return plan;
  }

  private WorkerPlan createWorkPlan2(int workers) {
    WorkerPlan plan = new WorkerPlan();
    for (int i = workers - 1; i >= 0; i--) {
      plan.addWorker(new Worker(i));
    }
    return plan;
  }

  private DataFlowTaskGraph createGraph(int parallel) {
    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    TaskGraphBuilder builder = TaskGraphBuilder.newBuilder(Config.newBuilder().build());
    builder.addSource("source", testSource, parallel);
    ComputeConnection c = builder.addSink("sink", testSink, parallel);
    c.reduce("source")
        .viaEdge("edge")
        .withOperation(Op.SUM, MessageTypes.INTEGER_ARRAY);
    builder.setMode(OperationMode.STREAMING);
    return builder.build();
  }

  private DataFlowTaskGraph createGraphWithGraphConstraints(int parallel) {
    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    TaskGraphBuilder builder = TaskGraphBuilder.newBuilder(Config.newBuilder().build());
    builder.addSource("source", testSource, parallel);
    ComputeConnection c = builder.addSink("sink", testSink, parallel);
    c.reduce("source").viaEdge("edge").withOperation(Op.SUM, MessageTypes.INTEGER_ARRAY);
    builder.setMode(OperationMode.STREAMING);

    builder.addGraphConstraints(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER, "16");
    DataFlowTaskGraph graph = builder.build();
    return graph;
  }

  private DataFlowTaskGraph createGraphWithComputeTaskAndConstraints(int parallel) {

    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestCompute testCompute = new TaskSchedulerClassTest.TestCompute();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    TaskGraphBuilder taskGraphBuilder = TaskGraphBuilder.newBuilder(Config.newBuilder().build());
    taskGraphBuilder.addSource("source", testSource, parallel);

    ComputeConnection computeConnection = taskGraphBuilder.addCompute(
        "compute", testCompute, parallel);
    ComputeConnection sinkComputeConnection = taskGraphBuilder.addSink(
        "sink", testSink, parallel);

    computeConnection.direct("source")
        .viaEdge("cdirect-edge")
        .withDataType(MessageTypes.OBJECT);

    sinkComputeConnection.direct("compute")
        .viaEdge("sdirect-edge")
        .withDataType(MessageTypes.OBJECT);

    taskGraphBuilder.setMode(OperationMode.STREAMING);

    taskGraphBuilder.addGraphConstraints(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER, "24");
    DataFlowTaskGraph taskGraph = taskGraphBuilder.build();
    return taskGraph;
  }
}