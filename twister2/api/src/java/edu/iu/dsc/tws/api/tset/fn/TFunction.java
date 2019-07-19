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
package edu.iu.dsc.tws.api.tset.fn;

import java.io.Serializable;
import java.util.Map;

import edu.iu.dsc.tws.api.tset.Cacheable;
import edu.iu.dsc.tws.api.tset.TSetContext;

/**
 * Base class for all the functions in TSet implementation
 */
public interface TFunction extends Serializable {

  /**
   * Prepare the function. Use this context in case you need to add/get inputs
   *
   * @param context CONTEXT
   */
  default void prepare(TSetContext context) {

  }

  /**
   * Gets the input value for the given key from the input map
   *
   * @param key the key to be retrieved
   * @return the object associated with the given key, null if the key is not present
   * @deprecated Use context instead!
   */
  @Deprecated
  default Object getInput(String key) {
    throw new UnsupportedOperationException("Inputs for TSets are only supported for functions"
        + "that extend from TBaseFunction.");
  }

  /**
   * Adds the given key value pair into the input map
   *
   * @param key the key to be added
   * @param input the value associated with the key
   * @deprecated Use context instead!
   */
  @Deprecated
  default void addInput(String key, Cacheable<?> input) {
    throw new UnsupportedOperationException("Inputs for TSets are only supported for functions"
        + "that extend from TBaseFunction.");
  }

  /**
   * Adds the given map into the input map
   *
   * @param map map that contains key, input pairs that need to be added into
   * the  input map
   * @deprecated Use context instead!
   */
  @Deprecated
  default void addInputs(Map<String, Cacheable<?>> map) {
    throw new UnsupportedOperationException("Inputs for TSets are only supported for functions"
        + "that extend from TBaseFunction.");
  }
}

