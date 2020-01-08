void call(String chart, Map opts = [:]) {
  Map containers = opts.containers ?: [:]
  String helmContainer = containers.helm ?: ''
  List<String> helmOpts = opts.helmOpts ?: ["--entrypoint=''"]

  println "[ace] - Got containers - ${containers}"

  aceContainer(helmContainer, helmOpts, [:]) {
    sh """
      set -u
      set -e

      mkdir chart

      # Set Helm Home
      export HELM_HOME=\$(pwd)
      export XDG_CACHE_HOME=\$HELM_HOME/cache
      export XDG_CONFIG_HOME=\$HELM_HOME/config
      export XDG_data_HOME=\$HELM_HOME/data
      helm repo add ace https://evry-ace.github.io/helm-charts
      helm repo update

      helm pull ${chart} --untar -d chart
      mv chart/${chart.split('/')[1]}/* target-data
    """
  }
}
