#!/usr/bin/env bash
version=$1
if [ -z "$version" ]; then
    version=1.0.1-SNAPSHOT
fi
cd ..
cd structure-datascope-dependencies
mvn clean deploy -P oss -Dmaven.test.skip=true -Drevision=$version