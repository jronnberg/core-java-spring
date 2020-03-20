# Plant Description Alarm - Service Description

## Abstract
This document describes an abstract service, which, if implemented by an application system, can be used to monitor alarms raised by the [Plant Description Engine] core system.

## Overview
This document describes an abstract Arrowhead Framework service meant to enable monitoring of alarms raised by [Plant Description Engine]([PDE]) core system.

The PDE gathers information about the presence of all specified systems in the active plant description. If a system is not present it raises an alarm. If it detects that an unknown system has registered a service in the service registry it also raises an alarm.

This service is produced by the PDE and can be consumed by, for example, a dash-board to manage the alarms that the PDE raises.

 

## Service Interfaces
This section lists the interfaces that must be exposed by the [PDE] in alphabetical order. In particular, each
subsection names an abstract interface, an input type, an output type and a set of possible exceptions, in that
order. The input type is named inside parentheses, while the output type is preceded by a colon. Input and
output types are only denoted when accepted or returned, respectively, by the interface in question.
All abstract data types named in this section are defined in the [Information model](#information-model) section.

### interface GetAllPDEAlarms(): [PDEAlarmList](#struct-pdealarmlist)
Called to acquire a list of PDE alarms raised by the PDE.

### interface GetPDEAlarm(id): [PDEAlarm](#struct-pdealarm)
Called to acquire the __[PDEAlarm](#struct-pdealarm)__ specified by the `id` parameter.

### interface UpdatePDEAlarm(id, [PDEAlarmUpdate](#struct-pdealarmupdate)): [PDEAlarm](#struct-pdealarm)
Called to update the PDE Alarm specified by the `id` parameter with the information in the `PDEAlarmUpdate` parameter.
The newly updated PDE Alarm is returned.

## Information model
Here, all data objects that can be part of Plant Description Alarm service calls are listed in alphabetic order. Note that each
subsection, which describes one type of object, begins with the struct or union keywords. The former is used to
denote a collection of named fields, each with its own data type, while the latter is used to express that a value
is allowed to be any one out of a number of listed variant types. As a complement to the explicitly defined types
in this section, there is also a list of implicit [primitive](#primitives) types,
which are used to represent things like dates.

### struct PDEAlarm
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

### struct PDEAlarmList

| Field | Type | Description | 
| ----- | ---- | ----------- |
| `count` | Number | Number of records found |
| `data` | Array | Array of [PDE alarms](#struct-pdealarm) |

### struct PDEAlarmUpdate

Currently only the following values can be updated. If a field is not present the current value will remain unchanged.

| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `acknowledged` | Boolean | Has the alarm been acknowledged by an operator | `false` ||

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
[PDE]:plant-description-engine-sysd.md
[Plant Description Engine]:plant-description-engine-sysd.md