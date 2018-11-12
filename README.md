# IHE XDS and openEHR Integration Prototype Server

## Combined Integrated Document Source/Repository and openEHR Server (CISROS)

**Disclaimer:** This code is research code and documents the prototype developed as part of my master's thesis.

The prototype server runs the integration specification proposed in my thesis. It offers openEHR REST API endpoints to communicate with clients. As well as IHE XDS endpoints to communicate with the Affinity Domain. For testing purposes this server includes a Document Registry actor.

A test client will be provided for more illustrative access. However, all configurations of ports and endpoints are static and meant for local use only.

## Origin
Forked from [IPF XDS tutorial](https://github.com/oehf/ipf/tree/d4d8807020e5d168b5905c7b7f953dfcd8ac8062/tutorials/xds).

## Dependencies

- Java 1.8
- Maven

## Run 

See [XDS tutorial](https://oehf.github.io/ipf/ipf-tutorials-xds/index.html) for more information.

Minimal example to build and run:

1. run ``mvn clean install assembly:assembly -Dmaven.test.failure.ignore=true``
2. unzip ``target/ipf-tutorials-xds-$VERSION-bin.zip``
3. run extracted ``startup.sh`` or ``startup.bat`` file

### Notes

Test have to be ignored when building. They are only working singly executed at the moment, because the test environment does not create isolated storages and therefore assumptions are failing.