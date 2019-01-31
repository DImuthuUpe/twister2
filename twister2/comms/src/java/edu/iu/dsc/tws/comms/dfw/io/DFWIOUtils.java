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
package edu.iu.dsc.tws.comms.dfw.io;

import java.nio.ByteBuffer;
import java.util.UUID;

import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.dfw.ChannelMessage;
import edu.iu.dsc.tws.comms.dfw.DataBuffer;
import edu.iu.dsc.tws.comms.dfw.OutMessage;
import static edu.iu.dsc.tws.comms.dfw.io.MultiMessageSerializer.HEADER_SIZE;

public final class DFWIOUtils {

  private DFWIOUtils() {
    throw new UnsupportedOperationException();
  }

  public static String getOperationName(int target, DataFlowOperation op) {
    String uid = op.getUniqueId();
    return "partition-" + uid + "-" + target + "-" + UUID.randomUUID().toString();
  }

  /**
   * Reset the serialize state
   * @param state state
   * @param completed weather completed
   * @return if completed
   */
  public static boolean resetState(SerializeState state, boolean completed) {
    if (completed) {
      // add the key size at the end to total size
      state.setBytesCopied(0);
      state.setBufferNo(0);
      state.setData(null);
      state.setPart(SerializeState.Part.INIT);
      state.setKeySize(0);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Builds the header of the message. The length value is inserted later so 0 is added as a place
   * holder value. The header structure is |source|flags|destinationID|length|
   *
   * @param buffer the buffer to which the header is placed
   * @param sendMessage the message that the header is build for
   */
  public static void buildHeader(DataBuffer buffer, OutMessage sendMessage) {
    if (buffer.getCapacity() < HEADER_SIZE) {
      throw new RuntimeException("The buffers should be able to hold the complete header");
    }
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    // now lets put the content of header in
    byteBuffer.putInt(sendMessage.getSource());
    // the path we are on, if not grouped it will be 0 and ignored
    byteBuffer.putInt(sendMessage.getFlags());
    // the destination id
    byteBuffer.putInt(sendMessage.getPath());
    // we set the number of messages
    byteBuffer.putInt(0);
    // lets set the size for 16 for now
    buffer.setSize(HEADER_SIZE);
  }

  /**
   * Builds the header of the message. The length value is inserted later so 0 is added as a place
   * holder value. The header structure is |source|flags|destinationID|length|
   *
   * @param buffer the buffer to which the header is placed
   * @param sendMessage the message that the header is build for
   * @param noOfMessages number of messages in this single message
   */
  public static void buildHeader(DataBuffer buffer, OutMessage sendMessage, int noOfMessages) {
    if (buffer.getCapacity() < HEADER_SIZE) {
      throw new RuntimeException("The buffers should be able to hold the complete header");
    }
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    // now lets put the content of header in
    byteBuffer.putInt(sendMessage.getSource());
    // the path we are on, if not grouped it will be 0 and ignored
    byteBuffer.putInt(sendMessage.getFlags());
    // the destination id
    byteBuffer.putInt(sendMessage.getPath());
    // we set the number of messages
    byteBuffer.putInt(noOfMessages);
    // lets set the size for 16 for now
    buffer.setSize(HEADER_SIZE);
  }

  /**
   * Create a copy of the channel message
   * @param channelMessage message
   * @return the copy
   */
  public static ChannelMessage createChannelMessageCopy(ChannelMessage channelMessage) {
    ChannelMessage copy = new ChannelMessage();
    //Values that are not copied: refCount,
    copy.setMessageDirection(channelMessage.getMessageDirection());
    copy.setReleaseListener(channelMessage.getReleaseListener());
    copy.setOriginatingId(channelMessage.getOriginatingId());
    copy.setHeader(channelMessage.getHeader());
    copy.setComplete(channelMessage.isComplete());
    copy.setDataType(channelMessage.getDataType());
    copy.setKeyType(channelMessage.getKeyType());
    copy.setHeaderSize(channelMessage.getHeaderSize());
    copy.setReceivedState(channelMessage.getReceivedState());
    copy.addBuffers(channelMessage.getNormalBuffers());
    copy.addOverFlowBuffers(channelMessage.getOverflowBuffers());

    return copy;
  }
}


