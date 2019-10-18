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
package edu.iu.dsc.tws.master.worker;

import com.google.protobuf.Message;

import edu.iu.dsc.tws.api.resource.ISenderToDriver;

public class JMSenderToDriver implements ISenderToDriver {

  private JMWorkerAgent workerAgent;

  public JMSenderToDriver(JMWorkerAgent workerAgent) {
    this.workerAgent = workerAgent;
  }

  @Override
  public boolean sendToDriver(Message message) {
    return workerAgent.sendWorkerToDriverMessage(message);
  }
}
