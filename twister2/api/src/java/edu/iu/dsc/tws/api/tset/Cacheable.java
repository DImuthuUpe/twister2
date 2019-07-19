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

package edu.iu.dsc.tws.api.tset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.iu.dsc.tws.api.dataset.DataObject;
import edu.iu.dsc.tws.api.dataset.DataPartition;

/**
 * All Tsets that are cachable need to implement this interface
 * This interface defines the methods that other classes can use to
 * access the cached data
 */
public interface Cacheable<T> extends Serializable {

  /**
   * retrieve data saved in the TSet
   *
   * @return dataObject
   */
  default List<T> getData() {
    List<T> results = new ArrayList<>();

    if (getDataObject() != null) {
      for (DataPartition<T> partition : getDataObject().getPartitions()) {
        while (partition.getConsumer().hasNext()) {
          results.add(partition.getConsumer().next());
        }
      }
    }

    return results;
  }

  /**
   * retrieve data saved in the TSet
   *
   * @return dataObject
   */
  DataObject<T> getDataObject();

  /*  *//**
   * get the data from the given partition
   *
   * This is not deprecated. Because a partition needs to be traversed using its consumer, and
   * can not return a value T from it!
   * @param partitionId the partition ID
   * @return the data related to the given partition
   *//*
  @Deprecated
  T getPartitionData(int partitionId);

  *//**
   * Add Data to the data object
   *
   * @param value value to be added
   * @return true if the data was added successfully or false otherwise
   *//*
  boolean addData(T value);*/

}
