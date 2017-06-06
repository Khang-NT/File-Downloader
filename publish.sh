#!/bin/bash
set -e
set -x

if [[ "$TRAVIS_TAG" =~ ^release-v[0-9]+(.[0-9]+)+$ ]];
then
    ./gradlew bintrayUpload -PreleaseTag=$TRAVIS_TAG;
fi;
