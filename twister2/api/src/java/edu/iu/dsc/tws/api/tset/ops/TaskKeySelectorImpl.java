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
package edu.iu.dsc.tws.api.tset.ops;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.task.TaskContext;
import edu.iu.dsc.tws.api.task.TaskKeySelector;
import edu.iu.dsc.tws.api.tset.fn.Selector;

public class TaskKeySelectorImpl<K, V> implements TaskKeySelector {
  private Selector<K, V> selector;

  public TaskKeySelectorImpl(Selector<K, V> selec) {
    this.selector = selec;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object select(Object data) {
    return selector.select((V) data);
  }

  @Override
  public void prepare(Config cfg, TaskContext context) {

  }
}
