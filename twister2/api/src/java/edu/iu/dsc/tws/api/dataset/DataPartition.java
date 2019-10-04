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

package edu.iu.dsc.tws.api.dataset;

import java.io.Serializable;

/**
 * Partition of a distributed set
 *
 * @param <T> partition
 */
public interface DataPartition<T> extends Serializable {

  /**
   * Get the data consumer
   *
   * @return the consumer
   */
  DataPartitionConsumer<T> getConsumer();

  /**
   * Returns the first data frame of this partition or defaultValue
   * if it doesn't exist
   */
  default T firstOrDefault(T defaultValue) {
    DataPartitionConsumer<T> consumer = this.getConsumer();
    return consumer.hasNext() ? consumer.next() : defaultValue;
  }

  /**
   * Returns the first data frame of this partition
   */
  default T first() {
    return this.firstOrDefault(null);
  }

  void setId(int id);

  /**
   * Get the id of the partition
   *
   * @return the id of the partition
   */
  int getPartitionId();

  default void clear() {

  }
}
