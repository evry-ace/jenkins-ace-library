package no.ace

class Notifier implements Serializable {
  static String commitAuthor(Object script) {
    String name

    try {
      name = script.sh script: 'git show -s --pretty=%an', returnStdout: true
    } catch (e) {
      name = 'Unknown'
    }

    return name
  }

  static String formatMessage(
    Object script,
    String buildStatus = 'STARTED',
    String buildSubject = ''
  ) {
    String buildUrl = script.env.BUILD_URL
    String subject
    String message

    if (buildSubject == '') {
      String jobName = script.env.JOB_NAME
      String buildNum = script.env.BUILD_NUMBER

      String commitAuthor = commitAuthor(script)
      subject = "${buildStatus}: Job ${jobName} build #${buildNum} by ${commitAuthor}"
    } else {
      subject = "${buildStatus}: ${subject}"
    }

    if (buildStatus == 'PENDING INPUT') {
      message = "${subject} <${buildUrl}input/|${buildUrl}input>"
    } else {
      message = "${subject} <${buildUrl}|${buildUrl}>"
    }

    return message
  }
}
