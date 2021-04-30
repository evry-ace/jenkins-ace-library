void call(Map target, Map opts = [:]) {
  Map containers = opts.containers ?: [:]
  String helmContainer = containers.helm ?: ''
  List<String> helmOpts = opts.helmOpts ?: ["--entrypoint=''"]
  String chart = target.chart
  String repo = target.repo ?: 'https://evry-ace.github.io/helm-charts'
  String repoName = target.repoName ?: 'ace'
  String version = target.version ?: ''

  if (!chart) {
    error('[ace] No chart specified, dying.')
  }

  println "[ace] - Got containers - ${containers}"

  aceContainerWrapper(helmContainer, helmOpts, [:]) {
    sh """
      set -u
      set -e

      rm -rf chart
      mkdir chart

      # Set Helm Home
      export HELM_HOME=\$(pwd)
      export XDG_CACHE_HOME=\$HELM_HOME/cache
      export XDG_CONFIG_HOME=\$HELM_HOME/config
      export XDG_data_HOME=\$HELM_HOME/data
      helm repo add ${repoName} ${repo}
      helm repo update

      helm pull ${chart} --version "${version}" --untar -d chart
      mv chart/${chart.split('/')[1]}/* target-data
    """
  }
}
