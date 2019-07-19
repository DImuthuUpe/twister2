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
package edu.iu.dsc.tws.api.tset.sets;

import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.api.tset.Cacheable;
import edu.iu.dsc.tws.api.tset.fn.MapFunc;
import edu.iu.dsc.tws.api.tset.fn.PartitionFunc;
import edu.iu.dsc.tws.api.tset.fn.ReduceFunc;
import edu.iu.dsc.tws.api.tset.link.TLink;

/**
 * Twister data set.
 *
 * @param <T> type of the data set
 */
public interface TSet<T> extends BuildableTSet {
  /**
   * Name of the tset
   */
  TSet<T> setName(String name);

  /**
   * Direct operation
   *
   * @return this TSet
   */
  TLink<?, T> direct();

  /**
   * Reduce operation on the data
   *
   * @param reduceFn the reduce function
   * @return this set
   */
  TLink<?, T> reduce(ReduceFunc<T> reduceFn);

  /**
   * All reduce operation
   *
   * @param reduceFn reduce function
   * @return this set
   */
  TLink<?, T> allReduce(ReduceFunc<T> reduceFn);

  /**
   * Partition the data according the to partition function
   *
   * @param partitionFn partition function
   * @return this set
   */
  TLink<?, T> partition(PartitionFunc<T> partitionFn);

  /**
   * Gather the set of values into a single partition
   *
   * @return this set
   */
  TLink<?, T> gather();

  /**
   * Gather the set of values into a single partition
   *
   * @return this set
   */
  TLink<?, T> allGather();

  /**
   * Select a set of values
   *
   * @param <K> the type for partitioning
   * @return grouped set
   */
  <K, V> TupleTSet<K, V, T> mapToTuple(MapFunc<Tuple<K, V>, T> mapToTupleFn);

  /**
   * Create a cloned dataset
   *
   * @return the cloned set
   */
  TLink<?, T> replicate(int replications);

  /**
   * Executes TSet and saves any generated data as a in-memory data object
   *
   * @return the resulting TSet
   */
//  CachedTSet<T> cache();
  TSet<T> cache();

  /**
   * Allows users to pass in other TSets as inputs for a TSet
   *
   * @param key the key used to store the given TSet
   * @param input the TSet to be added as an input
   * @return true if the input was added successfully or false otherwise
   */
  boolean addInput(String key, Cacheable<?> input);

}
