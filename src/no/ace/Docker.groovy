#!/usr/bin/env groovy
package no.ace

class Docker implements Serializable {
  Object script
  Map opts
  Boolean nameOnly
  Boolean useBranchForTag

  Docker(Object script, Map opts = [:]) {
    this.script = script
    this.opts = opts

    this.nameOnly = opts.nameOnly ?: false
    this.useBranchForTag = opts.useBranchForTag ?: false
  }

  String imageName() {
    List list = this.script.env.JOB_NAME.split('/')
    String name

    if (list.size() == 3) {
      if (this.nameOnly) {
        name = this.scrubName(list[1])
      } else {
        name = this.scrubName("${list[0]}/${list[1]}".replaceAll(/[^A-Za-z0-9-\/]/, '-'))
      }
    } else if (list.size() == 2) {
      name = this.scrubName(list[0])
    } else {
      name = this.scrubName(this.script.env.JOB_NAME)
    }

    return name
  }

  String branchTag() {
    return scrub(this.script.env.BRANCH_NAME)
  }

  String buildTag() {
    if (this.useBranchForTag) {
      return "${scrub(this.script.env.BRANCH_NAME)}-${this.script.env.BUILD_NUMBER}"
    } else {
      return script.sh(script: 'git show -s --pretty=%an', returnStdout: true)
    }
  }

  String scrub(String str) {
    return str.toLowerCase().replaceAll(/[^a-z0-9]/, '-')
  }

  String scrubName(String str) {
    return str.toLowerCase().replaceAll(/[^a-z0-9\/]/, '-')
  }

  String image(String registry = '') {
    String image

    if (registry != '') {
      image = "${registry}/${imageName()}:${buildTag()}"
    } else {
      image = "${imageName()}:${buildTag()}"
    }

    return image
  }
}
