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

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.comms.CommunicationContext;
import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.api.comms.channel.ChannelListener;
import edu.iu.dsc.tws.api.comms.channel.ChannelReceiver;
import edu.iu.dsc.tws.api.comms.channel.TWSChannel;
import edu.iu.dsc.tws.api.comms.messaging.ChannelMessage;
import edu.iu.dsc.tws.api.comms.messaging.ChannelMessageReleaseCallback;
import edu.iu.dsc.tws.api.comms.messaging.MessageDirection;
import edu.iu.dsc.tws.api.comms.messaging.MessageFlags;
import edu.iu.dsc.tws.api.comms.messaging.MessageHeader;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.comms.packing.DataBuffer;
import edu.iu.dsc.tws.api.comms.packing.MessageDeSerializer;
import edu.iu.dsc.tws.api.comms.packing.MessageSerializer;
import edu.iu.dsc.tws.api.config.Config;

public class ChannelDataFlowOperation implements ChannelListener, ChannelMessageReleaseCallback {
  private static final Logger LOG = Logger.getLogger(ChannelDataFlowOperation.class.getName());

  /**
   * The default path to be used
   */
  private static final int DEFAULT_PATH = -1;

  // the configuration
  private Config config;
  // the task plan
  private LogicalPlan instancePlan;

  /**
   * The edge used
   */
  private int edge;
  /**
   * The network channel
   */
  private TWSChannel channel;
  /**
   * Set of de-serializers
   */
  private Map<Integer, MessageDeSerializer> messageDeSerializer;

  /**
   * Set of serializers
   */
  private Map<Integer, MessageSerializer> messageSerializer;

  // we may have multiple routes throughus
  private MessageType dataType;

  /**
   * The key type
   */
  private MessageType keyType = MessageTypes.BYTE;
  /**
   * Receive data type
   */
  private MessageType receiveDataType;
  /**
   * Receive key type
   */
  private MessageType receiveKeyType;
  /**
   * Weather keys are involved
   */
  private boolean isKeyed = false;
  /**
   * Lock for serializing the operations
   */
  private Lock lock = new ReentrantLock();

  /**
   * Executor id
   */
  private int executor;
  /**
   * The send sendBuffers used by the operation
   */
  private Queue<DataBuffer> sendBuffers;

  /**
   * Receive availableBuffers, for each receive we need to make
   */
  private Map<Integer, Queue<DataBuffer>> receiveBuffers;

  /**
   * Local buffers that are used when receive buffers need to be freed. Buffer are only added
   * to the list when needed
   */
  private Queue<DataBuffer> localReceiveBuffers;

  /**
   * Pending send messages
   */
  private Map<Integer, ArrayBlockingQueue<OutMessage>> pendingSendMessagesPerSource;

  /**
   * Pending receives in case the receives are not ready
   */
  private Map<Integer, Queue<InMessage>> pendingReceiveMessagesPerSource;

  /**
   * Pending deserialization
   */
  private Map<Integer, Queue<InMessage>> pendingReceiveDeSerializations;

  /**
   * Non grouped current messages, workerId, source, inMessage
   */
  private Map<Integer, InMessage> currentMessages = new HashMap<>();

  /**
   * These are the workers from which we receive messages
   */
  private Set<Integer> receivingExecutors;

  /**
   * The message receiver for MPI messages
   */
  private ChannelReceiver receiver;

  /**
   * Send communicationProgress tracker
   */
  private ProgressionTracker sendProgressTracker;

  /**
   * Deserialize communicationProgress track
   */
  private ProgressionTracker receiveProgressTracker;

  private AtomicInteger externalSendsPending = new AtomicInteger(0);

  ChannelDataFlowOperation(TWSChannel channel) {
    this.channel = channel;
  }

  public void init(Config cfg, MessageType messageType, MessageType rcvDataType,
                   MessageType kType, MessageType rcvKeyType, LogicalPlan plan,
                   int graphEdge, Set<Integer> recvExecutors,
                   ChannelReceiver msgReceiver,
                   Map<Integer, ArrayBlockingQueue<OutMessage>> pendingSendPerSource,
                   Map<Integer, Queue<InMessage>> pRMPS,
                   Map<Integer, Queue<InMessage>> pendingReceiveDesrialize,
                   Map<Integer, MessageSerializer> serializer,
                   Map<Integer, MessageDeSerializer> deSerializer, boolean keyed) {
    this.config = cfg;
    this.instancePlan = plan;
    this.edge = graphEdge;
    this.dataType = messageType;
    this.receiveDataType = rcvDataType;
    this.receiveKeyType = rcvKeyType;
    this.keyType = kType;
    this.executor = instancePlan.getThisWorker();
    this.receivingExecutors = recvExecutors;
    this.receiver = msgReceiver;
    this.isKeyed = keyed;

    this.pendingReceiveMessagesPerSource = pRMPS;
    this.pendingSendMessagesPerSource = pendingSendPerSource;
    this.pendingReceiveDeSerializations = pendingReceiveDesrialize;

    this.messageSerializer = serializer;
    this.messageDeSerializer = deSerializer;

    int noOfSendBuffers = CommunicationContext.sendBuffersCount(config);
    int sendBufferSize = CommunicationContext.bufferSize(config);

    this.sendBuffers = new ArrayBlockingQueue<>(noOfSendBuffers);
    for (int i = 0; i < noOfSendBuffers; i++) {
      sendBuffers.offer(new DataBuffer(channel.createBuffer(sendBufferSize)));
    }
    this.receiveBuffers = new HashMap<>();
    this.localReceiveBuffers = new ArrayDeque<>();

    LOG.log(Level.FINE, String.format("%d setup communication", instancePlan.getThisWorker()));
    // now setup the sends and receives
    setupCommunication();

    // initialize the serializers
    LOG.fine(String.format("%d setup initializers", instancePlan.getThisWorker()));
    initSerializers();

    initProgressTrackers();
  }

  public void init(Config cfg, MessageType messageType, LogicalPlan plan,
                   int graphEdge, Set<Integer> recvExecutors,
                   ChannelReceiver msgReceiver,
                   Map<Integer, ArrayBlockingQueue<OutMessage>> pendingSendPerSource,
                   Map<Integer, Queue<InMessage>> pRMPS,
                   Map<Integer, Queue<InMessage>> pendingReceiveDesrialize,
                   Map<Integer, MessageSerializer> serializer,
                   Map<Integer, MessageDeSerializer> deSerializer, boolean keyed) {
    init(cfg, messageType, messageType, keyType, keyType,
        plan, graphEdge, recvExecutors, msgReceiver,
        pendingSendPerSource, pRMPS, pendingReceiveDesrialize, serializer, deSerializer, keyed);
  }

  public void init(Config cfg, MessageType messageType, MessageType recvMessageType,
                   LogicalPlan plan,
                   int graphEdge, Set<Integer> recvExecutors,
                   ChannelReceiver msgReceiver,
                   Map<Integer, ArrayBlockingQueue<OutMessage>> pendingSendPerSource,
                   Map<Integer, Queue<InMessage>> pRMPS,
                   Map<Integer, Queue<InMessage>> pendingReceiveDesrialize,
                   Map<Integer, MessageSerializer> serializer,
                   Map<Integer, MessageDeSerializer> deSerializer, boolean keyed) {
    init(cfg, messageType, recvMessageType, keyType, keyType,
        plan, graphEdge, recvExecutors, msgReceiver,
        pendingSendPerSource, pRMPS, pendingReceiveDesrialize, serializer, deSerializer, keyed);
  }

  private void initSerializers() {
    // initialize the serializers
    for (MessageSerializer serializer : messageSerializer.values()) {
      serializer.init(config, sendBuffers);
    }
    for (MessageDeSerializer deSerializer : messageDeSerializer.values()) {
      deSerializer.init(config);
    }
  }

  private void initProgressTrackers() {
    Set<Integer> sendItems = pendingSendMessagesPerSource.keySet();
    sendProgressTracker = new ProgressionTracker(sendItems);

    Set<Integer> receiveItems = pendingReceiveMessagesPerSource.keySet();
    Set<Integer> desrializeItems = pendingReceiveDeSerializations.keySet();
    Set<Integer> items = new HashSet<>(receiveItems);
    items.addAll(desrializeItems);

    receiveProgressTracker = new ProgressionTracker(items);
  }

  /**
   * Setup the receives and send sendBuffers
   */
  private void setupCommunication() {
    // we will receive from these
    int maxReceiveBuffers = CommunicationContext.receiveBufferCount(config);
    int receiveBufferSize = CommunicationContext.bufferSize(config);
    for (Integer recv : receivingExecutors) {
      Queue<DataBuffer> recvList = new LinkedBlockingQueue<>();
      for (int i = 0; i < maxReceiveBuffers; i++) {
        recvList.add(new DataBuffer(channel.createBuffer(receiveBufferSize)));
      }
      // register with the channel
      LOG.fine(instancePlan.getThisWorker() + " Register to receive from: " + recv);
      channel.receiveMessage(0, recv, edge, this, recvList);
      receiveBuffers.put(recv, recvList);
    }

    // configure the send sendBuffers
    int sendBufferSize = CommunicationContext.bufferSize(config);
    int sendBufferCount = CommunicationContext.sendBuffersCount(config);
    for (int i = 0; i < sendBufferCount; i++) {
      DataBuffer buffer = new DataBuffer(channel.createBuffer(sendBufferSize));
      sendBuffers.offer(buffer);
    }
  }

  /**
   * Sends a message from a partial location
   *
   * @param source source id
   * @param message the actual message
   * @param target an specific target
   * @param flags message flags
   * @param routingParameters routing parameter
   * @return true if the message is accepted
   */
  protected boolean sendMessagePartial(int source, Object message, int target,
                                       int flags, RoutingParameters routingParameters) {
    // for partial sends we use minus value to find the correct queue
    ArrayBlockingQueue<OutMessage> pendingSendMessages =
        pendingSendMessagesPerSource.get(source * -1 - 1);
    return offerForSend(source, message, target, flags,
        routingParameters, pendingSendMessages);
  }

  /**
   * Sends a message from a originating location
   *
   * @param source source id
   * @param message the actual message
   * @param target an specific target
   * @param flags message flags
   * @param routingParameters routing parameter
   * @return true if the message is accepted
   */
  public boolean sendMessage(int source, Object message, int target,
                             int flags, RoutingParameters routingParameters) {
    ArrayBlockingQueue<OutMessage> pendingSendMessages = pendingSendMessagesPerSource.get(source);
    if (pendingSendMessages == null) {
      throw new RuntimeException(String.format("%d No send messages %d", executor, source));
    }
    return offerForSend(source, message, target, flags,
        routingParameters, pendingSendMessages);
  }

  @Override
  public void onReceiveComplete(int id, int e, DataBuffer buffer) {
    // we need to try to build the message here, we may need many more messages to complete
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    byteBuffer.position(buffer.getSize());
    byteBuffer.flip();

    // we have the source of the message at 0th position as an integer
    int source = byteBuffer.getInt(Integer.BYTES); // since 0 has the size
    InMessage currentMessage = currentMessages.get(source);
    if (currentMessage == null) {
      MessageHeader header = messageDeSerializer.get(source).buildHeader(buffer, e);

      MessageType recvDType = receiveDataType;
      MessageType recvKType = receiveKeyType;

      if ((header.getFlags() & MessageFlags.SYNC_BARRIER) == MessageFlags.SYNC_BARRIER) {
        recvDType = MessageTypes.BYTE_ARRAY;
        recvKType = MessageTypes.EMPTY;
      }

      currentMessage = new InMessage(id, recvDType, this, header);
      if (isKeyed) {
        currentMessage.setKeyType(recvKType);
      }
      if (!currentMessage.addBufferAndCalculate(buffer)) {
        currentMessages.put(source, currentMessage);
      }
      // we add the message immediately to the deserialization as we can deserialize partially
      Queue<InMessage> deserializeQueue = pendingReceiveDeSerializations.get(source);
      if (!deserializeQueue.offer(currentMessage)) {
        throw new RuntimeException(executor + " We should have enough space: "
            + deserializeQueue.size());
      }
    } else {
      if (currentMessage.addBufferAndCalculate(buffer)) {
        currentMessages.remove(source);
      }
    }
  }

  /**
   * Weather we have more data to complete
   */
  public boolean isComplete() {
    /*for (Map.Entry<Integer, Queue<InMessage>> e
        : pendingReceiveMessagesPerSource.entrySet()) {
      if (e.getValue().size() > 0) {
        return false;
      }
    }*/

    for (Map.Entry<Integer, ArrayBlockingQueue<OutMessage>> e
        : pendingSendMessagesPerSource.entrySet()) {
      if (e.getValue().size() > 0) {
        return false;
      }
    }

//    for (Map.Entry<Integer, Queue<InMessage>> e : pendingReceiveDeSerializations.entrySet()) {
//      if (e.getValue().size() > 0) {
//        return false;
//      }
//    }

    return externalSendsPending.get() == 0;
  }

  /**
   * Progress the serializations and receives, this method must be called by threads to
   * send messages through this communication
   */
  public void progress() {
    if (sendProgressTracker.canProgress()) {
      int sendId = sendProgressTracker.next();
      if (sendId != Integer.MIN_VALUE) {
        sendProgress(pendingSendMessagesPerSource.get(sendId), sendId);
        sendProgressTracker.finish(sendId);
      }
    }

    if (receiveProgressTracker.canProgress()) {
      int deserializeId = receiveProgressTracker.next();
      if (deserializeId != Integer.MIN_VALUE) {
        Queue<InMessage> msgQueue = pendingReceiveDeSerializations.get(deserializeId);
        if (msgQueue != null) {
          receiveDeserializeProgress(msgQueue, deserializeId);
        }

        Queue<InMessage> pendingReceiveMessages =
            pendingReceiveMessagesPerSource.get(deserializeId);
        if (pendingReceiveMessages != null) {
          receiveProgress(pendingReceiveMessages);
        }
        receiveProgressTracker.finish(deserializeId);
      }
    }
  }

  /**
   * Put the message into internal queues, to be serialized and then send to the network channel
   *
   * @param source source
   * @param message data
   * @param target target
   * @param flags flags
   * @param routingParameters routing parameters
   * @param pendingSendMessages the message queue
   * @return true if message is accepted
   */
  private boolean offerForSend(int source, Object message, int target, int flags,
                               RoutingParameters routingParameters,
                               ArrayBlockingQueue<OutMessage> pendingSendMessages) {
    if (pendingSendMessages.remainingCapacity() > 0) {
      int path = DEFAULT_PATH;
      if (routingParameters.getExternalRoutes().size() > 0) {
        path = routingParameters.getDestinationId();
      }

      OutMessage sendMessage = new OutMessage(source, edge,
          path, target, flags, routingParameters.getInternalRoutes(),
          routingParameters.getExternalRoutes(), dataType, keyType, this, message);

      // now try to put this into pending
      return pendingSendMessages.offer(sendMessage);
    }
    return false;
  }

  /**
   * Go through the out messages, create channel messages by using the serializer send them
   *
   * @param pendingSendMessages the pending message queue
   * @param sendId send target
   */
  private void sendProgress(Queue<OutMessage> pendingSendMessages, int sendId) {
    boolean canProgress = true;

    while (pendingSendMessages.size() > 0 && canProgress) {
      // take out pending messages
      OutMessage outMessage = pendingSendMessages.peek();
      Object data = outMessage.getData();

      // first lets send the message to internal destinations
      canProgress = sendInternally(outMessage, data);

      if (canProgress) {
        // we don't have an external executor to send this message
        if (outMessage.getExternalSends().size() == 0) {
          pendingSendMessages.poll();
          continue;
        }
        Queue<ChannelMessage> channelMessages = outMessage.getChannelMessages();
        // at this point lets build the message
        ChannelMessage serializeMessage = (ChannelMessage)
            messageSerializer.get(sendId).build(outMessage.getData(), outMessage);
        if (serializeMessage != null) {
          // we are incrementing the reference count here
          channelMessages.offer(serializeMessage);
        }

        ChannelMessage chMessage = channelMessages.peek();
        if (chMessage == null) {
          break;
        }

        List<Integer> externalRoutes = outMessage.getExternalSends();
        // okay we build the message, send it
        if (outMessage.getSendState() == OutMessage.SendState.SERIALIZED) {
          int startOfExternalRouts = chMessage.getAcceptedExternalSends();
          canProgress = sendExternally(outMessage, chMessage, externalRoutes, startOfExternalRouts);
          if (chMessage.getAcceptedExternalSends() == externalRoutes.size()) {
            // we are done
            pendingSendMessages.poll();
            channelMessages.poll();
            // the send is completed, we need to notify
            receiver.sendCompleted(outMessage);
          }
        } else if (outMessage.getSendState() == OutMessage.SendState.PARTIALLY_SERIALIZED) {
          int startOfExternalRouts = chMessage.getAcceptedExternalSends();

          canProgress = sendExternally(outMessage, chMessage, externalRoutes, startOfExternalRouts);
          if (chMessage.getAcceptedExternalSends() == externalRoutes.size()) {
            // we are done sending this channel message
            channelMessages.poll();
          }
        } else {
          break;
        }
      }
    }
  }

  private boolean sendExternally(OutMessage outMessage, ChannelMessage chMessage,
                                 List<Integer> exRoutes, int startOfExternalRouts) {
    boolean canProgress = true;
    lock.lock();
    try {
      if (!chMessage.isOutCountUpdated()) {
        chMessage.incrementRefCount(outMessage.getExternalSends().size());
        chMessage.setOutCountUpdated(true);
      }
      for (int i = startOfExternalRouts; i < exRoutes.size(); i++) {
        boolean sendAccepted = sendMessageToTarget(chMessage, exRoutes.get(i));
        // if no longer accepts stop
        if (!sendAccepted) {
          canProgress = false;
          break;
        } else {
          //remove the buffers from the original message
          chMessage.incrementAcceptedExternalSends();
          externalSendsPending.incrementAndGet();
        }
      }
    } finally {
      lock.unlock();
    }
    return canProgress;
  }

  private boolean sendInternally(OutMessage outMessage, Object messageObject) {
    boolean canProgress = true;
    if (outMessage.getSendState() == OutMessage.SendState.INIT) {
      // send it internally
      int startOfInternalRouts = outMessage.getAcceptedInternalSends();
      List<Integer> inRoutes = outMessage.getInternalSends();
      for (int i = startOfInternalRouts; i < outMessage.getInternalSends().size(); i++) {
        boolean receiveAccepted;
        lock.lock();
        try {
          receiveAccepted = receiver.receiveSendInternally(
              outMessage.getSource(), inRoutes.get(i), outMessage.getTarget(),
              outMessage.getFlags(), messageObject);
        } finally {
          lock.unlock();
        }
        if (!receiveAccepted) {
          canProgress = false;
          break;
        }
        outMessage.incrementAcceptedInternalSends();
      }
      if (canProgress) {
        outMessage.setSendState(OutMessage.SendState.SENT_INTERNALLY);
      }
    }
    return canProgress;
  }

  private void receiveDeserializeProgress(Queue<InMessage> msgQueue, int receiveId) {
    InMessage currentMessage = msgQueue.peek();
    if (currentMessage == null) {
      return;
    }

    if (currentMessage.getReceivedState() == InMessage.ReceivedState.INIT
        || currentMessage.getReceivedState() == InMessage.ReceivedState.BUILDING) {

      if (currentMessage.getReceivedState() == InMessage.ReceivedState.INIT) {
        Queue<InMessage> pendingReceiveMessages =
            pendingReceiveMessagesPerSource.get(currentMessage.getHeader().getSourceId());
        if (!pendingReceiveMessages.offer(currentMessage)) {
          throw new RuntimeException(executor + " We should have enough space: "
              + pendingReceiveMessages.size());
        }
        currentMessage.setReceivedState(InMessage.ReceivedState.BUILDING);
      }

      messageDeSerializer.get(receiveId).build(currentMessage,
          currentMessage.getHeader().getEdge());

      // lets check weather we have read everythong
      int readObjectNumber = currentMessage.getUnPkNumberObjects();
      // we need to get number of tuples and get abs because we are using -1 for single messages
      if (readObjectNumber == Math.abs(currentMessage.getHeader().getNumberTuples())) {
        currentMessage.setReceivedState(InMessage.ReceivedState.BUILT);
      }
    }

    // we remove only when the unpacking is complete and ready to receive
    if (currentMessage.getReceivedState() == InMessage.ReceivedState.BUILT
        || currentMessage.getReceivedState() == InMessage.ReceivedState.RECEIVE
        || currentMessage.getReceivedState() == InMessage.ReceivedState.DONE) {
      msgQueue.poll();
    }
  }

  private void receiveProgress(Queue<InMessage> pendingReceiveMessages) {
    boolean canProgress = true;
    while (pendingReceiveMessages.size() > 0 && canProgress) {
      InMessage currentMessage = pendingReceiveMessages.peek();

      lock.lock();
      try {
        if (currentMessage.getReceivedState() == InMessage.ReceivedState.BUILDING
            || currentMessage.getReceivedState() == InMessage.ReceivedState.BUILT) {
          while (currentMessage.getBuiltMessages().size() > 0) {
            // get the first channel message
            ChannelMessage msg = currentMessage.getBuiltMessages().peek();
            if (msg != null) {
              if (!receiver.handleReceivedChannelMessage(msg)) {
                canProgress = false;
                break;
              }
              ChannelMessage releaseMsg = currentMessage.getBuiltMessages().poll();
              Objects.requireNonNull(releaseMsg).release();
            }
          }

          if (currentMessage.getReceivedState() == InMessage.ReceivedState.BUILT
              && currentMessage.getBuiltMessages().size() == 0 && canProgress) {
            currentMessage.setReceivedState(InMessage.ReceivedState.RECEIVE);
          }
        }

        if (currentMessage.getReceivedState() == InMessage.ReceivedState.RECEIVE) {
          Object object = currentMessage.getDeserializedData();
          if (!receiver.receiveMessage(currentMessage.getHeader(), object)) {
            break;
          }
          currentMessage.setReceivedState(InMessage.ReceivedState.DONE);
          pendingReceiveMessages.poll();
        } else {
          break;
        }
      } finally {
        lock.unlock();
      }
    }
  }

  private boolean sendMessageToTarget(ChannelMessage channelMessage, int i) {
    int e = instancePlan.getWorkerForForLogicalId(i);
    return channel.sendMessage(e, channelMessage, this);
  }

  @Override
  public void release(ChannelMessage message) {
    if (message.doneProcessing()) {
      int originatingId = message.getOriginatingId();
      releaseTheBuffers(originatingId, message);
    }
  }

  @Override
  public void onSendComplete(int id, int messageStream, ChannelMessage message) {
    // ok we don't have anything else to do
    message.release();
    externalSendsPending.getAndDecrement();
  }

  private void releaseTheBuffers(int id, ChannelMessage message) {
    if (MessageDirection.IN == message.getMessageDirection()) {
      Queue<DataBuffer> list = receiveBuffers.get(id);
      for (DataBuffer buffer : message.getNormalBuffers()) {
        // we need to clean the buffer so it can be used again
        buffer.getByteBuffer().clear();
        if (!list.offer(buffer)) {
          throw new RuntimeException(String.format("%d Buffer release failed for target %d",
              executor, message.getHeader().getDestinationIdentifier()));
        }
      }
      if (message.getOverflowBuffers().size() > 0) {
        for (DataBuffer byteBuffer : message.getOverflowBuffers()) {
          byteBuffer.getByteBuffer().clear();
          if (!localReceiveBuffers.offer(byteBuffer)) {
            throw new RuntimeException(String.format("%d Local buffer release failed for target %d",
                executor, message.getHeader().getDestinationIdentifier()));
          }
        }
        message.getOverflowBuffers().clear();
      }
    } else if (MessageDirection.OUT == message.getMessageDirection()) {
      ArrayBlockingQueue<DataBuffer> queue = (ArrayBlockingQueue<DataBuffer>) sendBuffers;
      for (DataBuffer buffer : message.getNormalBuffers()) {
        // we need to clean the buffer so it can be used again
        buffer.getByteBuffer().clear();
        if (!queue.offer(buffer)) {
          throw new RuntimeException(String.format("%d Buffer release failed for source %d %d %d",
              executor, message.getOriginatingId(), queue.size(), queue.remainingCapacity()));
        }
      }
    }
  }

  public LogicalPlan getInstancePlan() {
    return instancePlan;
  }

  public Config getConfig() {
    return config;
  }

  public void setKeyType(MessageType keyType) {
    this.keyType = keyType;
  }

  public void close() {
    for (int exec : receivingExecutors) {
      channel.releaseBuffers(exec, edge);
    }
  }
}
