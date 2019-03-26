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
package edu.iu.dsc.tws.examples.comms.stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.stream.SGather;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.examples.comms.BenchWorker;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkUtils;
import edu.iu.dsc.tws.examples.utils.bench.Timing;
import edu.iu.dsc.tws.examples.verification.ResultsVerifier;
import edu.iu.dsc.tws.examples.verification.comparators.ListOfIntArraysComparator;
import static edu.iu.dsc.tws.examples.utils.bench.BenchmarkConstants.TIMING_ALL_RECV;
import static edu.iu.dsc.tws.examples.utils.bench.BenchmarkConstants.TIMING_MESSAGE_RECV;

public class SGatherExample extends BenchWorker {
  private static final Logger LOG = Logger.getLogger(SGatherExample.class.getName());

  private SGather gather;

  private boolean gatherDone = false;

  private ResultsVerifier<int[], List<int[]>> resultsVerifier;

  @Override
  protected void execute() {
    TaskPlan taskPlan = Utils.createStageTaskPlan(config, workerId,
        jobParameters.getTaskStages(), workerList);

    Set<Integer> sources = new HashSet<>();
    Integer noOfSourceTasks = jobParameters.getTaskStages().get(0);
    for (int i = 0; i < noOfSourceTasks; i++) {
      sources.add(i);
    }
    int target = noOfSourceTasks;

    // create the communication
    gather = new SGather(communicator, taskPlan, sources, target, MessageType.INTEGER,
        new FinalReduceReceiver(jobParameters.getIterations(),
            jobParameters.getWarmupIterations()));


    Set<Integer> tasksOfExecutor = Utils.getTasksOfExecutor(workerId, taskPlan,
        jobParameters.getTaskStages(), 0);
    for (int t : tasksOfExecutor) {
      finishedSources.put(t, false);
    }
    if (tasksOfExecutor.size() == 0) {
      sourcesDone = true;
    }

    if (!taskPlan.getChannelsOfExecutor(workerId).contains(target)) {
      gatherDone = true;
    }

    this.resultsVerifier = new ResultsVerifier<>(inputDataArray, (dataArray, args) -> {
      List<int[]> listOfArrays = new ArrayList<>();
      for (int i = 0; i < noOfSourceTasks; i++) {
        listOfArrays.add(dataArray);
      }
      return listOfArrays;
    }, ListOfIntArraysComparator.getInstance());

    // now initialize the workers
    for (int t : tasksOfExecutor) {
      // the map thread where data is produced
      Thread mapThread = new Thread(new BenchWorker.MapWorker(t));
      mapThread.start();
    }
  }

  @Override
  protected void progressCommunication() {
    gather.progress();
  }

  @Override
  protected boolean sendMessages(int task, Object data, int flag) {
    while (!gather.gather(task, data, flag)) {
      // lets wait a litte and try again
      gather.progress();
    }
    return true;
  }

  @Override
  protected boolean isDone() {
    return gatherDone && sourcesDone && !gather.hasPending();
  }

  public class FinalReduceReceiver implements BulkReceiver {
    private int count = 0;
    private int expectedIterations;
    private int warmupIterations;

    public FinalReduceReceiver(int expected, int warmupIterations) {
      this.expectedIterations = expected;
      this.warmupIterations = warmupIterations;
    }

    @Override
    public void init(Config cfg, Set<Integer> expectedIds) {
    }

    @Override
    public boolean receive(int target, Iterator<Object> object) {
      count++;
      if (count > this.warmupIterations) {
        Timing.mark(TIMING_MESSAGE_RECV, workerId == 0);
      }

      //only do if verification is necessary, since this affects timing
      if (jobParameters.isDoVerify()) {
        List<int[]> dataReceived = new ArrayList<>();
        while (object.hasNext()) {
          Object data = object.next();
          if (data instanceof Tuple) {
            LOG.info(() -> String.format("%d received %d %d", target,
                (Integer) ((Tuple) data).getKey(), count));
            dataReceived.add((int[]) ((Tuple) data).getValue());
          } else {
            LOG.severe(() -> "Un-expected data: " + data.getClass());
          }
        }
        verifyResults(resultsVerifier, dataReceived, null);
      }

      if (count == expectedIterations + warmupIterations) {
        Timing.mark(TIMING_ALL_RECV, workerId == 0);
        BenchmarkUtils.markTotalAndAverageTime(resultsRecorder, workerId == 0);
        resultsRecorder.writeToCSV();
        LOG.info(() -> String.format("Target %d received count %d", target, count));
        gatherDone = true;
      }

      return true;
    }
  }
}
