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
package edu.iu.dsc.tws.api.comms;

import java.util.Iterator;
import java.util.Set;

import edu.iu.dsc.tws.api.config.Config;

/**
 * Receiver for multiple values
 */
public interface BulkReceiver {
  /**
   * Initialize the receiver
   * @param cfg configuration
   * @param targets expected targets
   */
  void init(Config cfg, Set<Integer> targets);

  /**
   * Receive to specific target
   * @param target the target
   * @param it iterator with messages
   */
  boolean receive(int target, Iterator<Object> it);

  /**
   * A sync event has occurred
   * @param target the target id
   * @return true if sync event is accepted
   */
  default boolean sync(int target, byte[] message) {
    return true;
  }
}
