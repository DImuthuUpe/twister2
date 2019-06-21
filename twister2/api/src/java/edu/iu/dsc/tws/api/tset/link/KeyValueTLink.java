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

package edu.iu.dsc.tws.api.tset.link;

import com.google.common.reflect.TypeToken;

import edu.iu.dsc.tws.api.tset.Selector;
import edu.iu.dsc.tws.api.tset.TSetEnv;
import edu.iu.dsc.tws.common.config.Config;

public abstract class KeyValueTLink<K, V> extends BaseTLink<V> {
  public KeyValueTLink(Config cfg, TSetEnv tSetEnv) {
    super(cfg, tSetEnv);
  }

  Class<? super K> getClassK() {
    return new TypeToken<K>(getClass()) {
    }.getRawType();
  }

  Class<? super V> getClassV() {
    return new TypeToken<V>(getClass()) {
    }.getRawType();
  }

  //todo use generics
  public abstract Selector getSelector();
}