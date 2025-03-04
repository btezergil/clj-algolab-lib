# btezergil/algolab-lib

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.btezergil/algolab-lib.svg)](https://clojars.org/org.clojars.btezergil/algolab-lib)

Clojure library to access Denizbank ALGOLAB infrastructure.

## Usage

You will have to initialize three environment variables for ALGOLAB:
- ALGOLAB_APIKEY: apiKey given by ALGOLAB for access
- ALGOLAB_USERNAME: TCKN of ALGOLAB account
- ALGOLAB_PASSWORD: password of ALGOLAB account

In order to access the API, you'll need to call the login function and then the login-sms-code function with the received code.
The hash received from this flow is required to generate the checker, which is used to validate all API requests.

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build a JAR (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.btezergil/algolab-lib on clojars.org by default.

## License

Copyright © 2025 Berke Tezergil

_EPLv1.0 is just the default for projects generated by `deps-new`: you are not_
_required to open source this project, nor are you required to use EPLv1.0!_
_Feel free to remove or change the `LICENSE` file and remove or update this_
_section of the `README.md` file!_

Distributed under the Eclipse Public License version 1.0.
