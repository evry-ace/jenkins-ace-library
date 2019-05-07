#!/usr/bin/env groovy
/*
  Utility to take a set of parameters generated for spinnaker
  webhook / pubsub and flatten it to a properties file instead.
*/
List<String> call(Map params, List<Map> artifacts) {
  return Spinnaker.paramsToProperties(params, artifacts)
}
