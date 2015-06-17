# Trapperkeeper Scheduler Service

[![Build Status](https://travis-ci.org/puppetlabs/trapperkeeper-scheduler.svg)](https://travis-ci.org/puppetlabs/trapperkeeper-scheduler)

[![Clojars Project](http://clojars.org/puppetlabs/trapperkeeper-scheduler/latest-version.svg)](http://clojars.org/puppetlabs/trapperkeeper-scheduler)

A Trapperkeeper service that provides a simple API for scheduling background tasks.

Other Trapperkeeper services may specify a dependency on the Scheduler service,
and then use its functions to schedule and cancel jobs to be run on background
worker threads.  The Scheduler service is responsible for managing the life cycle
of the underlying scheduling subsystem, thread pools, etc.

For more information, please see the [documentation](./documentation).

## License

Copyright © 2015 Puppet Labs
