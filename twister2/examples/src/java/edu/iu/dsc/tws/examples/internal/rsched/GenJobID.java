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
package edu.iu.dsc.tws.examples.internal.rsched;

import edu.iu.dsc.tws.api.util.JobIDUtils;

public final class GenJobID {

  private GenJobID() { }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("You must provide jobName as a parameter");
    }

    String userName = "au";
    String jobName = args[0];
    String jobID = JobIDUtils.generateJobID(jobName, userName);
    System.out.println(jobID);
  }
}
