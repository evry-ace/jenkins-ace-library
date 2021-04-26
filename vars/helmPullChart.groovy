void call(String chart, String helmChartVersion, Map opts = [:]) {
  Map containers = opts.containers ?: [:]
  String helmContainer = containers.helm ?: ''
  List<String> helmOpts = opts.helmOpts ?: ["--entrypoint=''"]

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
      helm repo add ace https://evry-ace.github.io/helm-charts
      helm repo update

      helm pull ${chart} --untar -d chart  -version ${helmChartVersion}
      mv chart/${chart.split('/')[1]}/* target-data
    """
  }
}
