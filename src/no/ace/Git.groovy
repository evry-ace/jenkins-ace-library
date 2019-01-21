#!/usr/bin/env groovy
package no.ace

class Git {
  static Boolean isMasterBranch(Map env) {
    return env.BRANCH_NAME == 'master'
  }

  static Boolean isDevelopBranch(Map env) {
    return env.BRANCH_NAME == 'develop'
  }

  static Boolean isFeatureBranch(Map env) {
    return (env.BRANCH_NAME =~ /^feature\//) != false
  }

  static Boolean isReleaseBranch(Map env) {
    return (env.BRANCH_NAME =~ /^release\/(v[0-9]+\.[0-9]+\.[0-9]+)/) != false
  }

  static String releaseBranchVersion(Map env) {
    Matcher m = env.BRANCH_NAME =~ /^release\/(v[0-9]+\.[0-9]+\.[0-9]+)/
    return m ? m.group(1) : ''
  }

  static Boolean isPR(Map env) {
    return env.containsKey('CHANGE_ID')
  }

  static String prId(Map env) {
    return env.CHANGE_ID ?: ''
  }

  static String prUrl(Map env) {
    return env.CHANGE_URL ?: ''
  }
}
