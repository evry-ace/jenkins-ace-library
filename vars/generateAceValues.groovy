/*
  This macro will generate out all the deployment metadata / values needed
  to push to a gitops repo or to use helm directly
*/
void call(Map opts = [:]) {
  Map containers = opts.containers ?: [:]
  String parserContainer = containers.ace ?: ''

  List<String> parserOpts = [
    "-v ${pwd()}:/src",
  ]

  println "[ace] - Got containers - ${containers}"
  println "[ace] Will generate values & target metadata using   ${parserContainer}"

  aceContainer(parserContainer, parserOpts, [:]) {
    println "[ace] - Got opts - ${parserOpts}"

    sh """
    mkdir target-data
    python3 /app/ace-parser.py --ace ace.yaml --img-url ${opts.image} --output=target-data
    ls target-data
    """
  }
}
