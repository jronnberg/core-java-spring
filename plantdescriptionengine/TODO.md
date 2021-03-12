# Plant Description Engine, TODO

## Monitor service
* Remove `include` field from DTOs, merge with the included Plant Descriptions.

## Management service
* Require that metadata be null if consumer is true on a PD system port.
* Ensure that the consumer port used in a connection really is a consumer port.
* Add service interface to ports.
* Ensure that connected ports have the same service interface and
  service definition.

## Documentation
* Specify the cases where some fields may not be present (AR Kalix's default
  way of handling missing Optionals).
* Document the use of metadata.
* Document the priority field of connections

## Persistent data
* Use MySQL instead of files to store persistent data.