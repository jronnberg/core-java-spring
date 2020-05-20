# Plant Description Monitor - Service Description

## Abstract
This document describes an abstract service, which, if implemented by an application system, can be used to monitor a plant and related alarms raised by the [Plant Description Engine] core system.

## Overview
This document describes an abstract Arrowhead Framework service meant to enable monitoring of a plant and related alarms raised by [Plant Description Engine]([PDE]) core system.

The PDE gathers information about the presence of all specified systems in the active plant description. If a system is not present it raises an alarm. If it detects that an unknown system has registered a service in the service registry it also raises an alarm.

The PDE can, tentatively, also gather system data and inventory data about the systems in the plant. If that is gathered it can be retrieved using this service.

This service is produced by the PDE and can be consumed by, for example, a dash-board to manage the alarms that the PDE raises.


## Service Interfaces
This section lists the interfaces that must be exposed by the [PDE] in alphabetical order. In particular, each
subsection names an abstract interface, an input type, an output type and a set of possible exceptions, in that
order. The input type is named inside parentheses, while the output type is preceded by a colon. Input and
output types are only denoted when accepted or returned, respectively, by the interface in question.
All abstract data types named in this section are defined in the [Information model](#information-model) section.

### interface GetAllPdeAlarms(): [PdeAlarmList](#struct-pdealarmlist)
Called to acquire a list of PDE alarms raised by the PDE.

### interface GetPdeAlarm(id): [PdeAlarm](#struct-pdealarm)
Called to acquire the __[PdeAlarm](#struct-pdealarm)__ specified by the `id` parameter.

### interface UpdatePdeAlarm(id, [PdeAlarmUpdate](#struct-pdealarmupdate)): [PdeAlarm](#struct-pdealarm)
Called to update the PDE Alarm specified by the `id` parameter with the information in the `PdeAlarmUpdate` parameter.
The newly updated PDE Alarm is returned.

### interface GetAllPlantDescriptions(): [PlantDescriptionEntryList](#struct-plantdescriptionentrylist)

Called to acquire a list of Plant Description Entries present in the PDE.

### interface GetPlantDescription(id): [PlantDescriptionEntry](#struct-plantdescriptionentry)

Called to acquire the Plant Description Entry specified by the `id` parameter.


## Information model
Here, all data objects that can be part of Plant Description Monitor service calls are listed in alphabetic order. Note that each
subsection, which describes one type of object, begins with the struct or union keywords. The former is used to
denote a collection of named fields, each with its own data type, while the latter is used to express that a value
is allowed to be any one out of a number of listed variant types.
Some of the types are identical to types defined in the [Plant Description Management Service Description] and are not redefined here.
As a complement to the explicitly defined types in this section, there is also a list of implicit [primitive](#primitives) types,
which are used to represent things like dates.

### struct PdeAlarm
| Field | Type | Description |
| ----- | ---- | ----------- |
| `id` | Number | Id of the alarm |
| `systemName` | String | Identity of the system |
| `acknowledged` | Boolean | Has the alarm been acknowledged by an operator |
| `severity` | String | One out of `indeterminate/critical/major/minor/warning/cleared` |
| `description` | String | Description of the problem |
| `raisedAt` | DateTime | When the alarm was first raised |
| `updatedAt` | DateTime | When the alarm was last updated |
| `clearedAt` | DateTime | When the alarm was cleared |
| `acknowledgedAt` | DateTime | When the alarm was acknowledged |

### struct PdeAlarmList

| Field | Type | Description |
| ----- | ---- | ----------- |
| `count` | Number | Number of records found |
| `data` | Array | Array of [PDE alarms](#struct-pdealarm) |

### struct PdeAlarmUpdate

Currently only the following values can be updated. If a field is not present the current value will remain unchanged.

| Field | Type | Description | Mandatory | Default value |
| ----- | ---- | ----------- | --------- | ------------- |
| `acknowledged` | Boolean | Has the alarm been acknowledged by an operator | `false` ||


### struct PlantDescriptionEntry

| Field | Type | Description | Mandatory | Note |
| ----- | ---- | ----------- | --------- | ------------- |
| `id` | Number | Id of the entry | `true` | |
| `plantDescription` | String | Plant description name| `true` | |
| `active` | Boolean | Is this the active plant description | `true` | |
| `systems` | Array\<[SystemEntry](#struct-systementry)> | Array with systems expected to be present in the plant | `true` ||
| `connections` | Array\<[Connection]> | Array with connection that should be populated into the Orchestrator | `true` ||
| `createdAt` | DateTime | Creation date of the entry | `true` | |
| `updatedAt` | DateTime | When the entry was last updated | `true` | |

### struct PlantDescriptionEntryList

| Field | Type | Description |
| ----- | ---- | ----------- |
| `count` | Number | Number of records found |
| `data` | Array\<[PlanDescriptionEntry](#struct-plantdescriptionentry)> | Array with Plant Description Entries |

### struct SystemEntry
| Field | Type | Description | Mandatory | Note |
| ----- | ---- | ----------- | --------- | ------------- |
| `systemName` | String | Identity of the system | `true` | |
| `metadata` | Custom | Metadata - key-value pairs | `false` | Only present if specified |
| `ports` | Array\<[Port]> | Array with service ports exposed by the system | `true` ||
| `systemData` | Custom | System specific data - key-value pairs | `false` | Only present if provided by [Monitorable] service of the system |
| `inventoryId` | String | The systems Id in an Inventory system | `false` | Only present if provided by [Monitorable] service of the system |
| `inventoryData` | Custom | Inventory specific data - key-value pairs | `false` | Only present if provided by [Inventory] system |


### Primitives
Types and structures mentioned throughout this document that are assumed to be available to implementations
of this service. The concrete interpretations of each of these types and structures must be provided by any IDD
document claiming to implement this service.

| Type | Description |
| ---- | ----------- |
| Array \<A> | An ordered collection of elements, where each element conforms to type A. |
| Boolean | One out of `true` or `false`. |
| DateTime | Pinpoints a specific moment in time. |
| Number | Any IEEE 754 binary64 floating point number, except for +Inf, -Inf and NaN. |
| String | An arbitrary UTF-8 string. |

[Inventory]:TBD
[Monitorable]:monitorable-sd.md
[PDE]:plant-description-engine-sysd.md
[Plant Description Engine]:plant-description-engine-sysd.md
[Plant Description Management Service Description]:plant-description-management-sd.md
[Connection]:plant-description-management-sd.md#struct-connection
[Port]:plant-description-management-sd.md#struct-port
