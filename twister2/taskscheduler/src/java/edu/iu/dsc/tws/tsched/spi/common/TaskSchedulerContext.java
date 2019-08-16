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
package edu.iu.dsc.tws.tsched.spi.common;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.config.Context;

/**
 * This class is to represent the default values for the task instances and task scheduler logical
 * container values.
 */
public class TaskSchedulerContext extends Context {

  private static final String TWISTER2_TASK_SCHEDULER_MODE_STREAMING = "twister2.taskscheduler"
      + ".streaming";
  private static final String TWISTER2_TASK_SCHEDULER_MODE_STREAMING_DEFAULT = "roundrobin";

  private static final String TWISTER2_TASK_SCHEDULER_MODE_BATCH = "twister2.taskscheduler"
      + ".batch";
  private static final String TWISTER2_TASK_SCHEDULER_MODE_BATCH_DEFAULT = "roundrobin";

  private static final String TWISTER2_TASK_SCHEDULER_CLASS_STREAMING = "twister2."
      + "taskscheduler.streaming.class";

  private static final String TWISTER2_TASK_SCHEDULER_CLASS_STREAMING_DEFAULT = "edu.iu.dsc.tws."
      + "tsched.streaming.roundrobin.RoundRobinTaskScheduler";

  private static final String TWISTER2_TASK_SCHEDULER_CLASS_BATCH = "twister2."
      + "taskscheduler.batch.class";

  private static final String TWISTER2_TASK_SCHEDULER_CLASS_BATCH_DEFAULT = "edu.iu.dsc.tws."
      + "tsched.batch.roundrobin.RoundRobinBatchTaskScheduler";

  private static final String TWISTER2_TASK_SCHEDULER_TASK_TYPE = "twister2."
      + "taskscheduler.task.type";
  private static final String TWISTER2_TASK_SCHEDULING_TASK_TYPE_DEFAULT = "streaming";

  private static final String TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_RAM = "twister2."
      + "taskscheduler.task.instance.ram";
  private static final double TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_RAM_DEFAULT = 512.0;

  private static final String TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_DISK = "twister2."
      + "taskscheduler.task.instance.disk";
  private static final double TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_DISK_DEFAULT = 500.0;

  private static final String TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_CPU = "twister2."
      + "taskscheduler.task.instance.cpu";
  private static final double TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_CPU_DEFAULT = 2.0;

  private static final String TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_NETWORK = "twister2."
      + "taskscheduler.task.instance.network";
  private static final double TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_NETWORK_DEFAULT = 512.0;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_RAM = "twister2."
      + "taskscheduler.container.instance.ram";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_RAM_DEFAULT = 1024.0;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_DISK = "twister2."
      + "taskscheduler.container.instance.disk";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_DISK_DEFAULT = 1000.0;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_CPU = "twister2."
      + "taskscheduler.container.instance.cpu";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_CPU_DEFAULT = 2.0;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_NETWORK = "twister2."
      + "taskscheduler.container.instance.network";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_NETWORK_DEFAULT = 1024;

  private static final String TWISTER2_TASK_SCHEDULER_TASK_PARALLELISM = "twister2."
      + "taskscheduler.task.parallelism";
  private static final int TWISTER2_TASK_SCHEDULER_TASK_PARALLELISM_DEFAULT = 2;

  private static final String TWISTER2_TASK_SCHEDULER_NO_OF_INSTANCES_PER_CONTAINER = "twister2."
      + "taskscheduler.task.instances";
  private static final int TWISTER2_TASK_SCHEDULER_NO_OF_INSTANCES_PER_CONTAINER_DEFAULT = 2;

  private static final String TWISTER2_TASK_SCHEDULER_RAM_PADDING_PER_CONTAINER = "twister2."
      + "taskscheduler.ram.padding.container";
  private static final double TWISTER2_TASK_SCHEDULER_RAM_PADDING_PER_CONTAINER_DEFAULT = 2.0;

  private static final String TWISTER2_TASK_SCHEDULER_DISK_PADDING_PER_CONTAINER = "twister2."
      + "taskscheduler.disk.padding.container";
  private static final double TWISTER2_TASK_SCHEDULER_DISK_PADDING_PER_CONTAINER_DEFAULT = 12.0;

  private static final String TWISTER2_TASK_SCHEDULER_CPU_PADDING_PER_CONTAINER = "twister2."
      + "taskscheduler.cpu.padding.container";
  private static final double TWISTER2_TASK_SCHEDULER_CPU_PADDING_PER_CONTAINER_DEFAULT = 1.0;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_PADDING_PERCENTAGE = "twister2."
      + "taskscheduler.container.padding.percentage";
  private static final int TWISTER2_TASK_SCHEDULER_CONTAINER_PADDING_PERCENTAGE_DEFAULT = 1;

  private static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_BANDWIDTH = "twister2."
      + "taskscheduler.container.instance.bandwidth";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_BANDWIDTH_DEFAULT = 100;

  public static final String TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_LATENCY = "twister2."
      + "taskscheduler.container.instance.latency";
  private static final double TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_LATENCY_DEFAULT = 0.02;

  private static final String TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_BANDWIDTH = "twister2."
      + "taskscheduler.datanode.instance.bandwidth";
  private static final double TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_BANDWIDTH_DEFAULT = 200;

  private static final String TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_LATENCY = "twister2."
      + "taskscheduler.datanode.instance.latency";
  private static final double TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_LATENCY_DEFAULT = 0.01;

  public static String streamingTaskSchedulingMode(Config cfg) {
    return cfg.getStringValue(TWISTER2_TASK_SCHEDULER_MODE_STREAMING,
        TWISTER2_TASK_SCHEDULER_MODE_STREAMING_DEFAULT);
  }

  public static String batchTaskSchedulingMode(Config cfg) {
    return cfg.getStringValue(TWISTER2_TASK_SCHEDULER_MODE_BATCH,
        TWISTER2_TASK_SCHEDULER_MODE_BATCH_DEFAULT);
  }

  public static String streamingTaskSchedulingClass(Config cfg) {
    return cfg.getStringValue(TWISTER2_TASK_SCHEDULER_CLASS_STREAMING,
        TWISTER2_TASK_SCHEDULER_CLASS_STREAMING_DEFAULT);
  }

  public static String batchTaskSchedulingClass(Config cfg) {
    return cfg.getStringValue(TWISTER2_TASK_SCHEDULER_CLASS_BATCH,
        TWISTER2_TASK_SCHEDULER_CLASS_BATCH_DEFAULT);
  }

  public static String taskType(Config cfg) {
    return cfg.getStringValue(TWISTER2_TASK_SCHEDULER_TASK_TYPE,
        TWISTER2_TASK_SCHEDULING_TASK_TYPE_DEFAULT);
  }

  public static double taskInstanceRam(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_RAM,
        TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_RAM_DEFAULT);
  }

  public static double taskInstanceDisk(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_DISK,
        TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_DISK_DEFAULT);
  }

  public static double taskInstanceCpu(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_CPU,
        TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_CPU_DEFAULT);
  }

  public static double taskInstanceNetwork(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_NETWORK,
        TWISTER2_TASK_SCHEDULER_TASK_INSTANCE_NETWORK_DEFAULT);
  }

  public static double containerInstanceRam(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_RAM,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_RAM_DEFAULT);
  }

  public static double containerInstanceDisk(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_DISK,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_DISK_DEFAULT);
  }

  public static double containerInstanceCpu(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_CPU,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_CPU_DEFAULT);
  }

  public static double containerInstanceNetwork(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_NETWORK,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_NETWORK_DEFAULT);
  }

  public static double containerInstanceBandwidth(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_BANDWIDTH,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_BANDWIDTH_DEFAULT);
  }

  public static double containerInstanceLatency(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_LATENCY,
        TWISTER2_TASK_SCHEDULER_CONTAINER_INSTANCE_LATENCY_DEFAULT);
  }

  public static int taskParallelism(Config cfg) {
    return cfg.getIntegerValue(TWISTER2_TASK_SCHEDULER_TASK_PARALLELISM,
        TWISTER2_TASK_SCHEDULER_TASK_PARALLELISM_DEFAULT);
  }

  public static int defaultTaskInstancesPerContainer(Config cfg) {
    return cfg.getIntegerValue(TWISTER2_TASK_SCHEDULER_NO_OF_INSTANCES_PER_CONTAINER,
        TWISTER2_TASK_SCHEDULER_NO_OF_INSTANCES_PER_CONTAINER_DEFAULT);
  }

  public static double containerRamPadding(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_RAM_PADDING_PER_CONTAINER,
        TWISTER2_TASK_SCHEDULER_RAM_PADDING_PER_CONTAINER_DEFAULT);
  }

  public static double containerDiskPadding(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_DISK_PADDING_PER_CONTAINER,
        TWISTER2_TASK_SCHEDULER_DISK_PADDING_PER_CONTAINER_DEFAULT);
  }

  public static double containerCpuPadding(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_CPU_PADDING_PER_CONTAINER,
        TWISTER2_TASK_SCHEDULER_CPU_PADDING_PER_CONTAINER_DEFAULT);
  }

  public static int containerPaddingPercentage(Config cfg) {
    return cfg.getIntegerValue(TWISTER2_TASK_SCHEDULER_CONTAINER_PADDING_PERCENTAGE,
        TWISTER2_TASK_SCHEDULER_CONTAINER_PADDING_PERCENTAGE_DEFAULT);
  }

  public static double datanodeInstanceBandwidth(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_BANDWIDTH,
        TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_BANDWIDTH_DEFAULT);
  }

  public static double datanodeInstanceLatency(Config cfg) {
    return cfg.getDoubleValue(TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_LATENCY,
        TWISTER2_TASK_SCHEDULER_DATANODE_INSTANCE_LATENCY_DEFAULT);
  }

}

