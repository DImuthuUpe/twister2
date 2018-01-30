#!/bin/sh

set -eu

readonly MVN_GOAL="$1"
readonly VERSION_NAME="$2"
shift 2
readonly EXTRA_MAVEN_ARGS=("$@")

python $(dirname $0)/maven/generate_poms.py $VERSION_NAME \
  //twister2/common/src/java:config-java \
  //twister2/data/src/main/java:data-java \
  //twister2/common/src/java:common-java \
  //twister2/comms/src/java:comms-java \
  //twister2/api/src/java:api-java \
  //twister2/resource-scheduler/src/java:resource-scheduler-java \
  //twister2/proto:proto-resource-scheduler-java \
  //twister2/proto:proto-taskscheduleplan-java \
  //twister2/task/src/main/java:task-java 

library_output_file() {
  library=$1
  library_output=bazel-bin/$library
  if [[ ! -e $library_output ]]; then
     library_output=bazel-genfiles/$library
  fi
  if [[ ! -e $library_output ]]; then
    echo "Could not find bazel output file for $library"
    exit 1
  fi
  echo -n $library_output
}

deploy_library() {
  library=$1
  srcjar=$2
  javadoc=$3
  pomfile=$4
  bazel build $library $srcjar $javadoc

  mvn -e $MVN_GOAL \
    -Dfile=$(library_output_file $library) \
    -Djavadoc=$(library_output_file $javadoc) \
    -DpomFile=$pomfile \
    -Dsources=$(library_output_file $srcjar) \
    "${EXTRA_MAVEN_ARGS[@]:+${EXTRA_MAVEN_ARGS[@]}}"
}

deploy_library \
  twister2/data/src/main/java:libdata-java.jar \
  twister2/data/src/main/java:libdata-java.jar \
  twister2/data/src/main/java:libdata-java.jar \
  data.pom.xml

deploy_library \
  twister2/common/src/java:libcommon-java.jar \
  twister2/common/src/java:libcommon-java.jar \
  twister2/common/src/java:libcommon-java.jar \
  common.pom.xml

