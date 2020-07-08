# postman2jmx
Postman collection to Jmeter jmx file converter.

[![Build Status](https://api.travis-ci.com/eduardomioto/postman2jmx.svg?branch=master)](https://travis-ci.com/github/eduardomioto/postman2jmx)
[![Maintainability](https://api.codeclimate.com/v1/badges/bcb3647ef40cccaaef3b/maintainability)](https://codeclimate.com/github/eduardomioto/postman2jmx/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/bcb3647ef40cccaaef3b/test_coverage)](https://codeclimate.com/github/eduardomioto/postman2jmx/test_coverage)

### Note

- postman2jmx converter only converts the Postman V2+ exported files!
- feel free to send any pull requests.

### Installation

postman2jmx requires Java8+ and Maven 3+.

- Clone or download the project.
```sh
$ git clone https://github.com/Loadium/postman2jmx.git
```
- Build the project.
```sh
$ cd postman2jmx
$ mvn package
```

### Usage

- After build, go to the Postman2Jmx folder. It is located under the target folder.
```sh
$ cd target/Postman2Jmx
```
- Then execute the following command to convert your postman json collection file to the jmx file.
```sh
$ java -jar my_postman_collection.json my_jmx_file.jmx
```

License
----

The MIT License (MIT)
