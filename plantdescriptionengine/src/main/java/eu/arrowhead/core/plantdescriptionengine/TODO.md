# Plant Description Engine, TODO

## Management
* Separate settings for secure/insecure for providing and consuming services?

## Monitor service
* Send alarms if there is a mismatch between Plant Descriptions and available
  systems.
  * Currently, an alarm is created if there is a system missing. These alarms
    are never cleared.
* Add todo to repo
* Add example plant description to repo
* Remove `include` field from DTOs, merge with the included Plant Descriptions.
* Figure out how the authorization rules are supposed to be created. By the
  orchestrator, maybe?
* Usage of inventory

## Service Registry
* Clean up Aparajita's changes to `ServiceRegistryDBService.java`
* Regularly poll service registry for systems

## ISO 10303 inventory frontend
* Implement

## Documentation
* Specify the cases where some fields may not be present (AR Kalix's default
  way of handling missing Optionals).