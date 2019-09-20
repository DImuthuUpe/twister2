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

package edu.iu.dsc.tws.api.comms.channel;

import edu.iu.dsc.tws.api.comms.messaging.ChannelMessage;
import edu.iu.dsc.tws.api.comms.packing.DataBuffer;

/**
 * A listener for getting notifications about completion of network events
 */
public interface ChannelListener {
  /**
   * After a receive is complete this function gets called
   *
   * @param id the rank from which the receive happens
   */
  void onReceiveComplete(int id, int stream, DataBuffer message);

  /**
   * After a send is complete this function gets called
   *
   * @param id the rank from which the receive happens
   * @param message message
   */
  void onSendComplete(int id, int stream, ChannelMessage message);
}
