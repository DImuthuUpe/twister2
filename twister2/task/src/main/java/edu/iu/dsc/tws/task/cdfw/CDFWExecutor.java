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
package edu.iu.dsc.tws.task.cdfw;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Any;

import edu.iu.dsc.tws.common.driver.IDriverMessenger;
import edu.iu.dsc.tws.proto.system.job.CDFWJobAPI;

public final class CDFWExecutor {
  private static final Logger LOG = Logger.getLogger(CDFWExecutor.class.getName());

  /**
   * The queue to coordinate between driver and submitter
   */
  private BlockingQueue<DriverEvent> driverEvents = new LinkedBlockingDeque<>();

  /**
   * This submitter keeps track of state
   */
  private DriverState driverState = DriverState.INITIALIZE;

  /**
   * The driver messenger
   */
  private IDriverMessenger driverMessenger;

  /**
   * Execution env object to get the information about the workers
   */
  private CDFWEnv executionEnv;

  public CDFWExecutor(CDFWEnv executionEnv, IDriverMessenger messenger) {
    this.driverMessenger = messenger;
    this.executionEnv = executionEnv;
  }

  /**
   * The executeCDFW method first call the schedule method to get the schedule list of the CDFW.
   * Then, it invokes the build CDFW Job object to build the cdfw job object for the scheduled graphs.
   */
  public void execute(DataFlowGraph graph) {
    LOG.info("Starting task graph Requirements:" + graph.getGraphName());

    if (!(driverState == DriverState.JOB_FINISHED || driverState == DriverState.INITIALIZE)) {
      // now we need to send messages
      throw new RuntimeException("Invalid state to execute a job: " + driverState);
    }

    CDFWScheduler cdfwScheduler = new CDFWScheduler(this.executionEnv.getWorkerInfoList());
    Set<Integer> workerIDs = cdfwScheduler.schedule(graph);

    // this is the first time
    if (driverState == DriverState.INITIALIZE || driverState == DriverState.JOB_FINISHED) {
      try {
        DataFlowGraph dataFlowGraph = buildCDFWSchedulePlan(graph, workerIDs);
        CDFWJobAPI.SubGraph job = buildCDFWJob(dataFlowGraph);
        // now submit the job
        submitJob(job);
        driverState = DriverState.JOB_SUBMITTED;
        waitForEvent(DriveEventType.FINISHED_JOB);
        driverState = DriverState.JOB_FINISHED;
      } catch (Exception e) {
        throw new RuntimeException("Driver is not initialized", e);
      }
    } else {
      throw new RuntimeException("Failed to submit job while in this state: " + driverState);
    }
  }

  /**
   * The executeCDFW method first call the schedule method to get the schedule list of the CDFW.
   * Then, it invokes the buildCDFWJob method to build the job object for the scheduled graphs.
   *
   * @param graph the dataflow graph
   * @deprecated we are not using this method as of now
   */
  // Added to test and schedule multiple graphs at a time.
  //todo: implement this in different way, because this is not running the graphs in parallel
  @Deprecated
  public void executeCDFW(DataFlowGraph... graph) {

    if (!(driverState == DriverState.JOB_FINISHED || driverState == DriverState.INITIALIZE)) {
      // now we need to send messages
      throw new RuntimeException("Invalid state to execute a job: " + driverState);
    }

    CDFWScheduler cdfwScheduler = new CDFWScheduler(this.executionEnv.getWorkerInfoList());
    Map<DataFlowGraph, Set<Integer>> scheduleGraphMap = cdfwScheduler.schedule(graph);

    for (Map.Entry<DataFlowGraph, Set<Integer>> dataFlowGraphEntry : scheduleGraphMap.entrySet()) {
      // this is the first time
      if (driverState == DriverState.INITIALIZE || driverState == DriverState.JOB_FINISHED) {
        try {
          DataFlowGraph dataFlowGraph = dataFlowGraphEntry.getKey();
          Set<Integer> workerIDs = dataFlowGraphEntry.getValue();
          //build the schedule plan for the dataflow graph
          dataFlowGraph = buildCDFWSchedulePlan(dataFlowGraph, workerIDs);
          CDFWJobAPI.SubGraph job = buildCDFWJob(dataFlowGraph);
          //now submit the job
          submitJob(job);
          driverState = DriverState.JOB_SUBMITTED;
          // lets wait for another event
          waitForEvent(DriveEventType.FINISHED_JOB);
          driverState = DriverState.JOB_FINISHED;
        } catch (Exception e) {
          throw new RuntimeException("Driver is not initialized", e);
        }
      }
    }
  }

  void close() {
    // send the close message
    sendCloseMessage();
  }

  private DataFlowGraph buildCDFWSchedulePlan(DataFlowGraph dataFlowGraph,
                                              Set<Integer> workerIDs) {
    dataFlowGraph.setCdfwSchedulePlans(
        CDFWJobAPI.CDFWSchedulePlan.newBuilder().addAllWorkers(workerIDs).build());
    return dataFlowGraph;
  }

  private void sendCloseMessage() {
    CDFWJobAPI.CDFWJobCompletedMessage.Builder builder = CDFWJobAPI.CDFWJobCompletedMessage.
        newBuilder().setHtgJobname("");
    driverMessenger.broadcastToAllWorkers(builder.build());
  }

  /**
   * Send the job as a serialized protobuf to all the workers
   *
   * @param job subgraph
   */
  private void submitJob(CDFWJobAPI.SubGraph job) {
    LOG.log(Level.INFO, "Sending graph to workers for execution: " + job.getName());
    CDFWJobAPI.ExecuteMessage.Builder builder = CDFWJobAPI.ExecuteMessage.newBuilder();
    builder.setSubgraphName(job.getName());
    builder.setGraph(job);
    driverMessenger.broadcastToAllWorkers(builder.build());
  }

  /**
   * This method is responsible for building the cdfw job object which is based on the outcome of
   * the scheduled graphs list.
   */
  private CDFWJobAPI.SubGraph buildCDFWJob(DataFlowGraph job) {
    return job.build();
  }

  void workerMessageReceived(Any anyMessage, int senderWorkerID) {
    LOG.log(Level.INFO, String.format("Received worker message %d: %s", senderWorkerID,
        anyMessage.getClass().getName()));
    driverEvents.offer(new DriverEvent(DriveEventType.FINISHED_JOB, anyMessage, senderWorkerID));
  }


  private DriverEvent waitForEvent(DriveEventType type) throws Exception {
    // lets wait for driver events
    try {
      DriverEvent event = driverEvents.take();
      if (event.getType() != type) {
        throw new Exception("Un-expected event: " + type);
      }
      return event;
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to take event", e);
    }
  }
}
