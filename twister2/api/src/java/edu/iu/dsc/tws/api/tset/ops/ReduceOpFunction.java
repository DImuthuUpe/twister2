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

import edu.iu.dsc.tws.api.dataset.DataObject;
import edu.iu.dsc.tws.api.task.IFunction;
import edu.iu.dsc.tws.api.task.modifiers.Receptor;
import edu.iu.dsc.tws.api.tset.CacheableImpl;
import edu.iu.dsc.tws.api.tset.fn.ReduceFunction;

public class ReduceOpFunction<T> implements IFunction, Receptor {
  private static final long serialVersionUID = -4344592105191874L;

  private ReduceFunction<T> reduceFn;

  public ReduceOpFunction(ReduceFunction<T> reduceFn) {
    this.reduceFn = reduceFn;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object onMessage(Object object1, Object object2) {
    T t1 = (T) object1;
    T t2 = (T) object2;
    return reduceFn.reduce(t1, t2);
  }

  @Override
  public void add(String name, DataObject<?> data) {
    reduceFn.addInput(name, new CacheableImpl<>(data));
  }
}
