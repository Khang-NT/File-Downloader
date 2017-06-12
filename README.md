[![Build Status](https://travis-ci.org/Khang-NT/File-Downloader.svg?branch=master)](https://travis-ci.org/Khang-NT/File-Downloader)
[ ![Download](https://api.bintray.com/packages/khang-nt/maven/Java-File-Downloader/images/download.svg) ](https://bintray.com/khang-nt/maven/Java-File-Downloader/_latestVersion)

# Java File Downloader

This is a lightweight and flexible downloader library for Java. `FileDownloader` supports queue
processor to download parallel multiple files, dynamically split download task into segments with
[HTTP partial request](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests).

# Installation

Install via Maven:
```xml
<dependency>
  <groupId>com.github.khangnt</groupId>
  <artifactId>file-downloader</artifactId>
  <version>0.0.6</version>
  <type>pom</type>
</dependency>
```

Or Gradle:
```
compile 'com.github.khangnt:file-downloader:0.0.6'
```

# Overview

# Updates
See [CHANGELOG](CHANGELOG.md).

# License
File Downloader is licensed under an [Apache 2.0 license](LICENSE).