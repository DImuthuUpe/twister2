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
package edu.iu.dsc.tws.tsched.batch.datalocalityaware;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.config.Context;
import edu.iu.dsc.tws.data.fs.FileStatus;
import edu.iu.dsc.tws.data.fs.FileSystem;
import edu.iu.dsc.tws.data.fs.Path;
import edu.iu.dsc.tws.data.utils.DataNodeLocatorUtils;
import edu.iu.dsc.tws.data.utils.DataObjectConstants;
import edu.iu.dsc.tws.task.api.schedule.ContainerPlan;
import edu.iu.dsc.tws.task.api.schedule.Resource;
import edu.iu.dsc.tws.task.api.schedule.TaskInstancePlan;
import edu.iu.dsc.tws.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.task.graph.Vertex;
import edu.iu.dsc.tws.tsched.spi.common.TaskSchedulerContext;
import edu.iu.dsc.tws.tsched.spi.scheduler.Worker;
import edu.iu.dsc.tws.tsched.spi.scheduler.WorkerPlan;
import edu.iu.dsc.tws.tsched.spi.taskschedule.ITaskScheduler;
import edu.iu.dsc.tws.tsched.spi.taskschedule.InstanceId;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskInstanceMapCalculation;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;
import edu.iu.dsc.tws.tsched.utils.DataTransferTimeCalculator;
import edu.iu.dsc.tws.tsched.utils.TaskAttributes;
import edu.iu.dsc.tws.tsched.utils.TaskVertexParser;

/**
 * The data locality batch task scheduler generate the task schedule plan based on the distance
 * calculated between the worker node and the data nodes where the input data resides. Once the
 * allocation is done, it calculates the task instance ram, disk, and cpu values and
 * allocates the size of the container with required ram, disk, and cpu values.
 */
public class DataLocalityBatchTaskScheduler implements ITaskScheduler {

  private static final Logger LOG
      = Logger.getLogger(DataLocalityBatchTaskScheduler.class.getName());

  //Represents global task Id
  private static int globalTaskIndex = 0;

  //Represents the task instance ram
  private Double instanceRAM;

  //Represents the task instance disk
  private Double instanceDisk;

  //Represents the task instance cpu value
  private Double instanceCPU;

  //Config object
  private Config config;

  //WorkerId
  private int workerId;

  /**
   * This method first initialize the task instance values with default task instance ram, disk, and
   * cpu values from the task scheduler context.
   */
  @Override
  public void initialize(Config cfg) {
    this.config = cfg;
    this.instanceRAM = TaskSchedulerContext.taskInstanceRam(this.config);
    this.instanceDisk = TaskSchedulerContext.taskInstanceDisk(this.config);
    this.instanceCPU = TaskSchedulerContext.taskInstanceCpu(this.config);
  }

  public void initialize(Config cfg, int workerid) {
    this.workerId = workerid;
    this.config = cfg;
    this.instanceRAM = TaskSchedulerContext.taskInstanceRam(this.config);
    this.instanceDisk = TaskSchedulerContext.taskInstanceDisk(this.config);
    this.instanceCPU = TaskSchedulerContext.taskInstanceCpu(this.config);
  }


  /**
   * This is the base method for the data locality aware task scheduling for scheduling the batch
   * task instances. It retrieves the task vertex set of the task graph and send the set to the
   * data locality aware scheduling algorithm to allocate the batch task instances which are closer
   * to the data nodes.
   */
  @Override
  public TaskSchedulePlan schedule(DataFlowTaskGraph graph, WorkerPlan workerPlan) {

    TaskSchedulePlan taskSchedulePlan;

    Map<Integer, List<InstanceId>> containerInstanceMap;
    LinkedHashMap<Integer, ContainerPlan> containerPlans = new LinkedHashMap<>();

    LinkedHashSet<Vertex> taskVertexSet = new LinkedHashSet<>(graph.getTaskVertexSet());
    List<Set<Vertex>> taskVertexList = TaskVertexParser.parseVertexSet(graph);

    for (Set<Vertex> vertexSet : taskVertexList) {
      if (vertexSet.size() > 1) {
        containerInstanceMap = dataLocalityBatchSchedulingAlgorithm(vertexSet,
            workerPlan.getNumberOfWorkers(), workerPlan);
      } else {
        Vertex vertex = vertexSet.iterator().next();
        containerInstanceMap = dataLocalityBatchSchedulingAlgorithm(vertex,
            workerPlan.getNumberOfWorkers(), workerPlan);
      }

      TaskInstanceMapCalculation instanceMapCalculation = new TaskInstanceMapCalculation(
          this.instanceRAM, this.instanceCPU, this.instanceDisk);

      Map<Integer, Map<InstanceId, Double>> instancesRamMap = instanceMapCalculation.
          getInstancesRamMapInContainer(containerInstanceMap, taskVertexSet);

      Map<Integer, Map<InstanceId, Double>> instancesDiskMap = instanceMapCalculation.
          getInstancesDiskMapInContainer(containerInstanceMap, taskVertexSet);

      Map<Integer, Map<InstanceId, Double>> instancesCPUMap = instanceMapCalculation.
          getInstancesCPUMapInContainer(containerInstanceMap, taskVertexSet);

      for (int containerId : containerInstanceMap.keySet()) {

        double containerRAMValue = TaskSchedulerContext.containerRamPadding(config);
        double containerDiskValue = TaskSchedulerContext.containerDiskPadding(config);
        double containerCpuValue = TaskSchedulerContext.containerCpuPadding(config);

        List<InstanceId> taskInstanceIds = containerInstanceMap.get(containerId);
        Map<InstanceId, TaskInstancePlan> taskInstancePlanMap = new HashMap<>();

        for (InstanceId id : taskInstanceIds) {
          double instanceRAMValue = instancesRamMap.get(containerId).get(id);
          double instanceDiskValue = instancesDiskMap.get(containerId).get(id);
          double instanceCPUValue = instancesCPUMap.get(containerId).get(id);

          Resource instanceResource = new Resource(instanceRAMValue, instanceDiskValue,
              instanceCPUValue);

          taskInstancePlanMap.put(id, new TaskInstancePlan(
              id.getTaskName(), id.getTaskId(), id.getTaskIndex(), instanceResource));

          containerRAMValue += instanceRAMValue;
          containerDiskValue += instanceDiskValue;
          containerCpuValue += instanceDiskValue;
        }

        Worker worker = workerPlan.getWorker(containerId);
        Resource containerResource;

        if (worker != null && worker.getCpu() > 0 && worker.getDisk() > 0 && worker.getRam() > 0) {
          containerResource = new Resource((double) worker.getRam(),
              (double) worker.getDisk(), (double) worker.getCpu());
        } else {
          containerResource = new Resource(containerRAMValue, containerDiskValue,
              containerCpuValue);
        }

        ContainerPlan taskContainerPlan;
        if (containerPlans.containsKey(containerId)) {
          taskContainerPlan = containerPlans.get(containerId);
          taskContainerPlan.getTaskInstances().addAll(taskInstancePlanMap.values());
        } else {
          taskContainerPlan = new ContainerPlan(containerId, new HashSet<>(
              taskInstancePlanMap.values()), containerResource);
          containerPlans.put(containerId, taskContainerPlan);
        }
      }
    }
    //Represents the task schedule plan Id
    int taskSchedulePlanId = 0;
    taskSchedulePlan = new TaskSchedulePlan(
        taskSchedulePlanId, new HashSet<>(containerPlans.values()));

    //TODO: Just for validation purpose and it will be removed finally
    if (taskSchedulePlan != null) {
      Map<Integer, ContainerPlan> containersMap
          = taskSchedulePlan.getContainersMap();
      for (Map.Entry<Integer, ContainerPlan> entry : containersMap.entrySet()) {
        Integer integer = entry.getKey();
        ContainerPlan containerPlan = entry.getValue();
        Set<TaskInstancePlan> containerPlanTaskInstances
            = containerPlan.getTaskInstances();
        LOG.info("Task Details for Container Id:" + integer);
        for (TaskInstancePlan ip : containerPlanTaskInstances) {
          LOG.info("TaskId:" + ip.getTaskId() + "\tTask Index" + ip.getTaskIndex()
              + "\tTask Name:" + ip.getTaskName());
        }
      }
    }
    return taskSchedulePlan;
  }

  /**
   * This method is primarily responsible for generating the container and task instance map which
   * is based on the task graph, its configuration, and the allocated worker plan.
   */
  private Map<Integer, List<InstanceId>> dataLocalityBatchSchedulingAlgorithm(
      Vertex taskVertex, int numberOfContainers, WorkerPlan workerPlan) {

    DataNodeLocatorUtils dataNodeLocatorUtils = new DataNodeLocatorUtils(config);
    TaskAttributes taskAttributes = new TaskAttributes();

    int maxTaskInstancesPerContainer =
        TaskSchedulerContext.defaultTaskInstancesPerContainer(config);
    int cIdx = 0;
    int containerIndex;

    Map<Integer, List<InstanceId>> dataAwareAllocationMap = new HashMap<>();
    for (int i = 0; i < numberOfContainers; i++) {
      dataAwareAllocationMap.put(i, new ArrayList<>());
    }

    Map<String, Integer> parallelTaskMap = taskAttributes.getParallelTaskMap(taskVertex);
    Set<Map.Entry<String, Integer>> taskEntrySet = parallelTaskMap.entrySet();
    for (Map.Entry<String, Integer> aTaskEntrySet : taskEntrySet) {

      Map<String, List<DataTransferTimeCalculator>> workerPlanMap;
      String taskName = aTaskEntrySet.getKey();

      if (taskVertex.getName().equals(taskName)) {
        int totalTaskInstances = taskVertex.getParallelism();
        List<String> inputDataList = getInputFilesList();
        List<String> datanodesList = dataNodeLocatorUtils.findDataNodesLocation(inputDataList);

        workerPlanMap = calculateDistance(datanodesList, workerPlan, cIdx);
        List<DataTransferTimeCalculator> cal = findBestWorkerNode(workerPlanMap);

        /* This loop allocate the task instances to the respective container but, before allocation
        it will check whether the container has reached maximum task instance size which is
        able to hold. */
        for (int i = 0; i < totalTaskInstances; i++) {
          int maxContainerTaskObjectSize = 0;
          if (maxContainerTaskObjectSize < maxTaskInstancesPerContainer) {
            containerIndex = Integer.parseInt(cal.get(i).getNodeName());
            dataAwareAllocationMap.get(containerIndex).add(
                new InstanceId(taskVertex.getName(), globalTaskIndex, i));
            ++maxContainerTaskObjectSize;
          }
        }
        globalTaskIndex++;
      }
    }
    return dataAwareAllocationMap;
  }

  /**
   * This method generates the container and task instance map which is based on the task graph,
   * its configuration, and the allocated worker plan.
   *
   * @return Map
   */
  private Map<Integer, List<InstanceId>> dataLocalityBatchSchedulingAlgorithm(
      Set<Vertex> taskVertexSet, int numberOfContainers, WorkerPlan workerPlan) {

    TaskAttributes taskAttributes = new TaskAttributes();
    int cIdx = 0;
    int containerIndex;

    Map<String, Integer> parallelTaskMap = taskAttributes.getParallelTaskMap(taskVertexSet);
    Map<Integer, List<InstanceId>> dataAwareAllocationMap = new HashMap<>();
    Set<Map.Entry<String, Integer>> taskEntrySet = parallelTaskMap.entrySet();

    for (int i = 0; i < numberOfContainers; i++) {
      dataAwareAllocationMap.put(i, new ArrayList<>());
    }

    DataNodeLocatorUtils dataNodeLocatorUtils = new DataNodeLocatorUtils(config);
    for (Map.Entry<String, Integer> aTaskEntrySet : taskEntrySet) {
      Map<String, List<DataTransferTimeCalculator>> workerPlanMap;
      String taskName = aTaskEntrySet.getKey();
      /*If the vertex has the input data set list, get the status and path of the file in HDFS.*/
      for (Vertex vertex : taskVertexSet) {
        if (vertex.getName().equals(taskName)) {
          int totalNumberOfInstances = vertex.getParallelism();
          List<String> inputDataList = getInputFilesList();
          List<String> datanodesList = dataNodeLocatorUtils.findDataNodesLocation(inputDataList);
          workerPlanMap = calculateDistance(datanodesList, workerPlan, cIdx);
          List<DataTransferTimeCalculator> cal = findBestWorkerNode(workerPlanMap);
          for (int i = 0; i < totalNumberOfInstances; i++) {
            containerIndex = Integer.parseInt(cal.get(i).getNodeName().trim());
            dataAwareAllocationMap.get(containerIndex).add(new InstanceId(
                vertex.getName(), globalTaskIndex, i));
          }
          globalTaskIndex++;
        }
      }
    }
    return dataAwareAllocationMap;
  }

  private List<String> getInputFilesList() {

    List<String> inputDataList = new ArrayList<>();
    String directory = String.valueOf(config.get(DataObjectConstants.DINPUT_DIRECTORY));
    final Path path = new Path(directory + workerId);
    final FileSystem fileSystem;
    try {
      fileSystem = path.getFileSystem(config);
      if (config.get(DataObjectConstants.FILE_SYSTEM).equals(Context.TWISTER2_HDFS_FILESYSTEM)) {
        final FileStatus pathFile = fileSystem.getFileStatus(path);
        inputDataList.add(String.valueOf(pathFile.getPath()));
      } else if (config.get(DataObjectConstants.FILE_SYSTEM).equals(
          Context.TWISTER2_LOCAL_FILESYSTEM)) {
        for (FileStatus file : fileSystem.listFiles(path)) {
          String filename = String.valueOf(file.getPath());
          if (filename != null) {
            inputDataList.add(filename);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("IOException Occured");
    }
    return inputDataList;
  }

  /**
   * It calculates the distance between the data nodes and the worker nodes.
   */
  private Map<String, List<DataTransferTimeCalculator>> calculateDistance(
      List<String> datanodesList, WorkerPlan workerPlan, int taskIndex) {

    Map<String, List<DataTransferTimeCalculator>> workerPlanMap = new HashMap<>();
    Worker worker;
    double workerBandwidth;
    double workerLatency;
    double calculateDistance = 0.0;
    double datanodeBandwidth;
    double datanodeLatency;

    for (String nodesList : datanodesList) {
      ArrayList<DataTransferTimeCalculator> calculatedVal = new ArrayList<>();
      for (int i = 0; i < workerPlan.getNumberOfWorkers(); i++) {
        worker = workerPlan.getWorker(i);

        DataTransferTimeCalculator calculateDataTransferTime =
            new DataTransferTimeCalculator(nodesList, calculateDistance);

        if (worker.getProperty(Context.TWISTER2_BANDWIDTH) != null
            && worker.getProperty(Context.TWISTER2_LATENCY) != null) {
          workerBandwidth = (double) worker.getProperty(Context.TWISTER2_BANDWIDTH);
          workerLatency = (double) worker.getProperty(Context.TWISTER2_LATENCY);
        } else {
          workerBandwidth = TaskSchedulerContext.containerInstanceBandwidth(config);
          workerLatency = TaskSchedulerContext.containerInstanceLatency(config);
        }

        //Right now using the default configuration values
        datanodeBandwidth = TaskSchedulerContext.datanodeInstanceBandwidth(config);
        datanodeLatency = TaskSchedulerContext.datanodeInstanceLatency(config);

        //Calculate the distance between worker nodes and data nodes.
        calculateDistance = Math.abs((2 * workerBandwidth * workerLatency)
            - (2 * datanodeBandwidth * datanodeLatency));

        //(use this formula to calculate the data transfer time)
        //calculateDistance = File Size / Bandwidth;

        calculateDataTransferTime.setRequiredDataTransferTime(calculateDistance);
        calculateDataTransferTime.setNodeName(worker.getId() + "");
        calculateDataTransferTime.setTaskIndex(taskIndex);
        calculatedVal.add(calculateDataTransferTime);
      }
      workerPlanMap.put(nodesList, calculatedVal);
    }
    return workerPlanMap;
  }

  /**
   * This method finds the worker node which has better network parameters (bandwidth/latency)
   * or it will take lesser time for the data transfer if there is any.
   */
  private static List<DataTransferTimeCalculator> findBestWorkerNode(Map<String,
      List<DataTransferTimeCalculator>> workerPlanMap) {

    List<DataTransferTimeCalculator> cal = new ArrayList<>();
    for (Map.Entry<String, List<DataTransferTimeCalculator>> entry : workerPlanMap.entrySet()) {
      String key = entry.getKey();
      List<DataTransferTimeCalculator> value = entry.getValue();
      for (DataTransferTimeCalculator aValue : value) {
        cal.add(new DataTransferTimeCalculator(aValue.getNodeName(),
            aValue.getRequiredDataTransferTime(), key));
      }
    }
    return cal;
  }
}
