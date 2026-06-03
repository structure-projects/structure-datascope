#!/usr/bin/env bash
version=$1
if [ -z "$version" ]; then
    version=1.0.1
fi
cd ..
cd structure-datascope-dependencies
mvn clean install -Dmaven.test.skip=true -Drevision=$version