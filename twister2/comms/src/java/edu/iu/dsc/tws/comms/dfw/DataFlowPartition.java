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
package edu.iu.dsc.tws.comms.dfw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.io.MessageDeSerializer;
import edu.iu.dsc.tws.comms.dfw.io.MessageSerializer;
import edu.iu.dsc.tws.comms.dfw.io.UnifiedDeserializer;
import edu.iu.dsc.tws.comms.dfw.io.UnifiedKeyDeSerializer;
import edu.iu.dsc.tws.comms.dfw.io.UnifiedKeySerializer;
import edu.iu.dsc.tws.comms.dfw.io.UnifiedSerializer;
import edu.iu.dsc.tws.comms.routing.PartitionRouter;
import edu.iu.dsc.tws.comms.utils.KryoSerializer;
import edu.iu.dsc.tws.comms.utils.OperationUtils;
import edu.iu.dsc.tws.comms.utils.TaskPlanUtils;

public class DataFlowPartition implements DataFlowOperation, ChannelReceiver {
  private static final Logger LOG = Logger.getLogger(DataFlowPartition.class.getName());

  /**
   * Sources
   */
  private Set<Integer> sources;

  /**
   * Destinations
   */
  private Set<Integer> destinations;

  /**
   * Partition router
   */
  private PartitionRouter router;

  /**
   * Final receiver
   */
  private MessageReceiver finalReceiver;

  /**
   * Partial receiver
   */
  private MessageReceiver partialReceiver;

  /**
   * The actual implementation
   */
  private ChannelDataFlowOperation delegete;

  /**
   * Task plan
   */
  private TaskPlan instancePlan;

  /**
   * Receive message type, we can receive messages as just bytes
   */
  private MessageType receiveType;

  /**
   * Receive key type, we can receive keys as just bytes
   */
  private MessageType receiveKeyType;

  /**
   * Data type
   */
  private MessageType dataType;
  /**
   * Key type
   */
  private MessageType keyType;

  /**
   * Weather this is a key based communication
   */
  private boolean isKeyed;

  /**
   * Routing parameters are cached
   */
  private Table<Integer, Integer, RoutingParameters> routingParamCache = HashBasedTable.create();

  /**
   * Routing parameters are cached
   */
  private Table<Integer, Integer, RoutingParameters> partialRoutingParamCache
      = HashBasedTable.create();

  /**
   * Lock for progressing the communication
   */
  private Lock lock = new ReentrantLock();

  /**
   * Lock for progressing the partial receiver
   */
  private Lock partialLock = new ReentrantLock();

  /**
   * Edge used for communication
   */
  private int edge;

  /**
   * A place holder for keeping the internal and external destinations
   */
  private List<Integer> internalDestinations = new ArrayList<>();
  private List<Integer> externalDestinations = new ArrayList<>();

  public DataFlowPartition(TWSChannel channel, Set<Integer> sourceTasks, Set<Integer> destTasks,
                           MessageReceiver finalRcvr, MessageReceiver partialRcvr,
                           MessageType dataType, MessageType keyType) {
    this(channel, sourceTasks, destTasks, finalRcvr, partialRcvr);
    this.isKeyed = true;
    this.keyType = keyType;
    this.dataType = dataType;
    this.receiveKeyType = keyType;
    this.receiveType = dataType;
  }

  public DataFlowPartition(TWSChannel channel, Set<Integer> sourceTasks, Set<Integer> destTasks,
                           MessageReceiver finalRcvr, MessageReceiver partialRcvr,
                           MessageType dataType) {
    this(channel, sourceTasks, destTasks, finalRcvr, partialRcvr);
    this.dataType = dataType;
  }

  public DataFlowPartition(TWSChannel channel, Set<Integer> srcs,
                           Set<Integer> dests, MessageReceiver finalRcvr,
                           MessageReceiver partialRcvr) {
    this.sources = srcs;
    this.destinations = dests;
    this.delegete = new ChannelDataFlowOperation(channel);

    this.finalReceiver = finalRcvr;
    this.partialReceiver = partialRcvr;
  }

  public DataFlowPartition(Config cfg, TWSChannel channel, TaskPlan tPlan, Set<Integer> srcs,
                           Set<Integer> dests, MessageReceiver finalRcvr,
                           MessageReceiver partialRcvr,
                           MessageType dType, MessageType rcvType,
                           int e) {
    this(cfg, channel, tPlan, srcs, dests, finalRcvr, partialRcvr, dType, rcvType,
        null, null, e);
    this.isKeyed = false;
  }

  public DataFlowPartition(Config cfg, TWSChannel channel, TaskPlan tPlan, Set<Integer> srcs,
                           Set<Integer> dests, MessageReceiver finalRcvr,
                           MessageReceiver partialRcvr,
                           MessageType dType, MessageType rcvType,
                           MessageType kType, MessageType rcvKType,
                           int e) {
    this.instancePlan = tPlan;
    this.sources = srcs;
    this.destinations = dests;
    this.delegete = new ChannelDataFlowOperation(channel);
    this.dataType = dType;
    this.receiveType = rcvType;
    this.keyType = kType;
    this.receiveKeyType = rcvKType;
    this.edge = e;

    if (keyType != null) {
      this.isKeyed = true;
    }

    this.finalReceiver = finalRcvr;
    this.partialReceiver = partialRcvr;

    init(cfg, dType, instancePlan, edge);
  }

  /**
   * Initialize
   */
  public void init(Config cfg, MessageType t, TaskPlan taskPlan, int ed) {
    this.edge = ed;

    Set<Integer> thisSources = TaskPlanUtils.getTasksOfThisWorker(taskPlan, sources);
    int executor = taskPlan.getThisExecutor();
    LOG.log(Level.FINE, String.format("%d setup loadbalance routing %s %s",
        taskPlan.getThisExecutor(), sources, destinations));
    this.router = new PartitionRouter(taskPlan, sources, destinations);
    Map<Integer, Set<Integer>> internal = router.getInternalSendTasks();
    Map<Integer, Set<Integer>> external = router.getExternalSendTasks();
    this.instancePlan = taskPlan;
    this.dataType = t;
    if (this.receiveType == null) {
      this.receiveType = dataType;
    }

    LOG.log(Level.FINE, String.format("%d adding internal/external routing",
        taskPlan.getThisExecutor()));
    for (int s : thisSources) {
      Set<Integer> integerSetMap = internal.get(s);
      if (integerSetMap != null) {
        this.internalDestinations.addAll(integerSetMap);
      }

      Set<Integer> integerSetMap1 = external.get(s);
      if (integerSetMap1 != null) {
        this.externalDestinations.addAll(integerSetMap1);
      }
      LOG.fine(String.format("%d adding internal/external routing %d",
          taskPlan.getThisExecutor(), s));
      break;
    }

    LOG.log(Level.FINE, String.format("%d done adding internal/external routing",
        taskPlan.getThisExecutor()));
    this.finalReceiver.init(cfg, this, receiveExpectedTaskIds());
    this.partialReceiver.init(cfg, this, router.partialExpectedTaskIds());

    Map<Integer, ArrayBlockingQueue<Pair<Object, OutMessage>>> pendingSendMessagesPerSource =
        new HashMap<>();
    Map<Integer, Queue<Pair<Object, InMessage>>> pendingReceiveMessagesPerSource
        = new HashMap<>();
    Map<Integer, Queue<InMessage>> pendingReceiveDeSerializations = new HashMap<>();
    Map<Integer, MessageSerializer> serializerMap = new HashMap<>();
    Map<Integer, MessageDeSerializer> deSerializerMap = new HashMap<>();

    Set<Integer> srcs = TaskPlanUtils.getTasksOfThisWorker(taskPlan, sources);
    Set<Integer> tempsrcs = TaskPlanUtils.getTasksOfThisWorker(taskPlan, sources);

    //need to set minus tasks as well
    for (Integer src : tempsrcs) {
      srcs.add((src * -1) - 1);
    }
    for (int s : srcs) {
      // later look at how not to allocate pairs for this each time
      pendingSendMessagesPerSource.put(s, new ArrayBlockingQueue<>(
          DataFlowContext.sendPendingMax(cfg)));
      if (isKeyed) {
        serializerMap.put(s, new UnifiedKeySerializer(new KryoSerializer(), executor,
            keyType, dataType));
      } else {
        serializerMap.put(s, new UnifiedSerializer(new KryoSerializer(), executor, dataType));
      }
    }

    int maxReceiveBuffers = DataFlowContext.receiveBufferCount(cfg);
    int receiveExecutorsSize = receivingExecutors().size();
    if (receiveExecutorsSize == 0) {
      receiveExecutorsSize = 1;
    }
    Set<Integer> execs = router.receivingExecutors();
    for (int ex : execs) {
      int capacity = maxReceiveBuffers * 2 * receiveExecutorsSize;
      pendingReceiveMessagesPerSource.put(ex, new ArrayBlockingQueue<>(capacity));
      pendingReceiveDeSerializations.put(ex, new ArrayBlockingQueue<>(capacity));
      if (isKeyed) {
        deSerializerMap.put(ex, new UnifiedKeyDeSerializer(new KryoSerializer(),
            executor, keyType, dataType));
      } else {
        deSerializerMap.put(ex, new UnifiedDeserializer(new KryoSerializer(), executor, dataType));
      }
    }

    for (int src : srcs) {
      for (int dest : destinations) {
        sendRoutingParameters(src, dest);
      }
    }

    delegete.init(cfg, dataType, receiveType, keyType, receiveKeyType, taskPlan, edge,
        router.receivingExecutors(), this,
        pendingSendMessagesPerSource, pendingReceiveMessagesPerSource,
        pendingReceiveDeSerializations, serializerMap, deSerializerMap, isKeyed);
  }

  @Override
  public boolean sendPartial(int source, Object message, int flags) {
    int newFlags = flags | MessageFlags.ORIGIN_PARTIAL;
    return delegete.sendMessagePartial(source, message, 0,
        newFlags, sendPartialRoutingParameters(source, 0));
  }

  @Override
  public boolean sendPartial(int source, Object message, int flags, int target) {
    int newFlags = flags | MessageFlags.ORIGIN_PARTIAL;
    return delegete.sendMessagePartial(source, message, target, newFlags,
        sendPartialRoutingParameters(source, target));
  }

  @Override
  public boolean send(int source, Object message, int flags) {
    int newFlags = flags | MessageFlags.ORIGIN_SENDER;
    return delegete.sendMessage(source, message, 0, newFlags, sendRoutingParameters(source, 0));
  }

  @Override
  public boolean send(int source, Object message, int flags, int target) {
    int newFlags = flags | MessageFlags.ORIGIN_SENDER;
    return delegete.sendMessage(source, message, target, newFlags,
        sendRoutingParameters(source, target));
  }

  public boolean isComplete() {
    boolean done = delegete.isComplete();
    boolean needsFurtherProgress = OperationUtils.progressReceivers(delegete, lock, finalReceiver,
        partialLock, partialReceiver);
    return done && !needsFurtherProgress;
  }

  public boolean isDelegateComplete() {
    return delegete.isComplete();
  }

  @Override
  public boolean progress() {
    return OperationUtils.progressReceivers(delegete, lock, finalReceiver,
        partialLock, partialReceiver);
  }

  @Override
  public void close() {
    if (partialReceiver != null) {
      partialReceiver.close();
    }

    if (finalReceiver != null) {
      finalReceiver.close();
    }

    delegete.close();
  }

  @Override
  public void clean() {
    if (partialReceiver != null) {
      partialReceiver.clean();
    }

    if (finalReceiver != null) {
      finalReceiver.clean();
    }
  }

  @Override
  public void finish(int source) {
    for (int dest : destinations) {
      // first we need to call finish on the partial receivers
      while (!send(source, new byte[0], MessageFlags.END, dest)) {
        // lets progress until finish
        progress();
      }
    }
  }

  @Override
  public TaskPlan getTaskPlan() {
    return instancePlan;
  }

  @Override
  public String getUniqueId() {
    return String.valueOf(edge);
  }

  private RoutingParameters sendRoutingParameters(int source, int path) {
    if (routingParamCache.contains(source, path)) {
      return routingParamCache.get(source, path);
    } else {
      RoutingParameters routingParameters = new RoutingParameters();
      routingParameters.setDestinationId(path);
      routingParameters.addInteranlRoute(source);
      routingParamCache.put(source, path, routingParameters);
      return routingParameters;
    }
  }

  private RoutingParameters sendPartialRoutingParameters(int source, int destination) {
    if (partialRoutingParamCache.contains(source, destination)) {
      return partialRoutingParamCache.get(source, destination);
    } else {
      RoutingParameters routingParameters = new RoutingParameters();
      routingParameters.setDestinationId(destination);
      if (externalDestinations.contains(destination)) {
        routingParameters.addExternalRoute(destination);
      } else {
        routingParameters.addInteranlRoute(destination);
      }
      partialRoutingParamCache.put(source, destination, routingParameters);
      return routingParameters;
    }
  }

  public boolean receiveSendInternally(int source, int path,
                                       int destination, int flags, Object message) {
    // okay this must be for the
    if ((flags & MessageFlags.ORIGIN_PARTIAL) == MessageFlags.ORIGIN_PARTIAL) {
      return finalReceiver.onMessage(source, path, destination, flags, message);
    }
    return partialReceiver.onMessage(source, path, destination, flags, message);
  }

  protected Set<Integer> receivingExecutors() {
    return router.receivingExecutors();
  }

  protected Map<Integer, List<Integer>> receiveExpectedTaskIds() {
    return router.receiveExpectedTaskIds();
  }

  public boolean receiveMessage(MessageHeader header, Object object) {
    return finalReceiver.onMessage(header.getSourceId(), DataFlowContext.DEFAULT_DESTINATION,
        header.getDestinationIdentifier(), header.getFlags(), object);
  }

  public Set<Integer> getSources() {
    return sources;
  }

  public Set<Integer> getDestinations() {
    return destinations;
  }

  @Override
  public MessageType getKeyType() {
    return keyType;
  }

  @Override
  public MessageType getDataType() {
    return dataType;
  }

}
