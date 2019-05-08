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

import edu.iu.dsc.tws.comms.api.MessageType;

/**
 * Keyed content is serialized given priority and serialized as two parts of key and object.
 */
public class Tuple<K, V> {
  private K key;

  private V value;

  private MessageType keyType;

  private MessageType contentType;

  public Tuple() {
  }

  public Tuple(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public Tuple(K k, V data,
               MessageType keyType, MessageType dataType) {
    this.key = k;
    this.value = data;
    this.keyType = keyType;
    this.contentType = dataType;
  }

  public static <K, V> Tuple of(K key, V value,
                                MessageType<K, ?> keyType,
                                MessageType<V, ?> valueType) {
    return new Tuple<>(key, value, keyType, valueType);
  }

  public static <K, V> Tuple of(K key, V value) {
    return new Tuple<>(key, value);
  }

  public MessageType getKeyType() {
    return keyType;
  }

  public void setKeyType(MessageType keyType) {
    this.keyType = keyType;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  public MessageType getContentType() {
    return contentType;
  }

  public void setKey(K key) {
    this.key = key;
  }

  public void setValue(V value) {
    this.value = value;
  }

  public void setContentType(MessageType contentType) {
    this.contentType = contentType;
  }

  @Override
  public String toString() {
    return "KeyValue{"
        + "key="
        + key
        + ", value="
        + value
        + '}';
  }
}
