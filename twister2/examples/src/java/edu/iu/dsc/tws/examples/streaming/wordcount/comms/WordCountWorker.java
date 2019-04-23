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
package edu.iu.dsc.tws.examples.streaming.wordcount.comms;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.net.Network;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.controller.IWorkerController;
import edu.iu.dsc.tws.common.exceptions.TimeoutException;
import edu.iu.dsc.tws.common.worker.IPersistentVolume;
import edu.iu.dsc.tws.common.worker.IVolatileVolume;
import edu.iu.dsc.tws.common.worker.IWorker;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.MessageTypes;
import edu.iu.dsc.tws.comms.api.Op;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.functions.reduction.ReduceOperationFunction;
import edu.iu.dsc.tws.comms.api.selectors.HashingSelector;
import edu.iu.dsc.tws.comms.api.stream.SKeyedReduce;
import edu.iu.dsc.tws.examples.utils.WordCountUtils;

public class WordCountWorker implements IWorker {
  private static final Logger LOG = Logger.getLogger(WordCountWorker.class.getName());

  private SKeyedReduce keyGather;

  private Communicator channel;

  private static final int NO_OF_TASKS = 8;

  private Config config;

  private int id;

  private int noOfTasksPerExecutor;

  private Set<Integer> sources;
  private Set<Integer> destinations;
  private TaskPlan taskPlan;

  @Override
  public void execute(Config cfg, int workerID,
                      IWorkerController workerController,
                      IPersistentVolume persistentVolume,
                      IVolatileVolume volatileVolume) {
    this.config = cfg;
    this.id = workerID;
    this.noOfTasksPerExecutor = NO_OF_TASKS / workerController.getNumberOfWorkers();

    setupTasks(workerController);
    setupNetwork(workerController);

    // create the communication
    keyGather = new SKeyedReduce(channel, taskPlan, sources, destinations,
        MessageTypes.OBJECT, MessageTypes.INTEGER,
        new ReduceOperationFunction(Op.SUM, MessageTypes.INTEGER),
        new WordAggregate(), new HashingSelector());

    scheduleTasks();
    progress();
  }

  private void setupTasks(IWorkerController workerController) {
    try {
      taskPlan = WordCountUtils.createWordCountPlan(config, id,
          workerController.getAllWorkers(), NO_OF_TASKS);
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return;
    }

    sources = new HashSet<>();
    for (int i = 0; i < NO_OF_TASKS / 2; i++) {
      sources.add(i);
    }
    destinations = new HashSet<>();
    for (int i = 0; i < NO_OF_TASKS / 2; i++) {
      destinations.add(NO_OF_TASKS / 2 + i);
    }
    LOG.fine(String.format("%d sources %s destinations %s",
        taskPlan.getThisExecutor(), sources, destinations));
  }

  private void setupNetwork(IWorkerController controller) {
    TWSChannel twsChannel = Network.initializeChannel(config, controller);
    this.channel = new Communicator(config, twsChannel);
  }

  private void scheduleTasks() {
    if (id < 2) {
      for (int i = 0; i < noOfTasksPerExecutor; i++) {
        // the map thread where data is produced
        Thread mapThread = new Thread(new StreamingWordSource(keyGather, 1000,
            noOfTasksPerExecutor * id + i, 10));
        mapThread.start();
      }
    }
  }

  private void progress() {
    // we need to communicationProgress the communication
    while (true) {
      try {
        // communicationProgress the channel
        channel.getChannel().progress();
        // we should communicationProgress the communication directive
        keyGather.progress();
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "Error", t);
      }
    }
  }
}
