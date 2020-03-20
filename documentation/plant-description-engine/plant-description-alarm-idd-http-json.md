# Plant Description Alarm HTTP(S)/JSON - Interface Design Description

## Abstract
This document describes the HTTP/{TLS}/JSON variant of a service that can be used to monitor alarms raised by the [Plant Description Engine] core system.

## Overview
This document describes the HTTP/{TLS}/JSON variant of the
[Plant Description Alarm] ([PDA]) service, which allows for arbitrary
Arrowhead Framework systems to monitor alarms raised by the [PDE] core system.
Readers of this document are assumed to be familiar with the [PDA] service.
For more information about the service, please refer to the service description document [PDA].
The rest of this document describes how to realize the [PDA] service using [HTTP], optionally with [TLS], and [JSON], both in terms of its [interfaces](#service-interfaces) and its [information model](#information-model).

## Service Interfaces
This section describes the interfaces that must be exposed by [Plant Description Alarm] services. In particular, the below
subsection first names the HTTP method and path used to call the interface, after which it names an abstract
interface from the [Plant Description Alarm] service description document, output type, as well as errors that can be thrown. The
interface is expected to respond with HTTP status code 200 OK for all successful calls. 

### GET {baseURI}/alarm
 - __Interface:	GetAllPDAAlarms__
 - __Output: [PDEAlarmList](#pdealarmlist)__
	 
Called to acquire a list of PDE alarms raised by the PDE.

Returns a list of PDE alarms. If `page` and `item_per_page` are not defined, returns
all records. 

Query params:

| Field | Description | Mandatory |
| ----- | ----------- | --------- |
| `page` | zero based page index | no |
| `item_per_page` | maximum number of items returned | no |
| `sort_field` | sorts by the given column | no |
| `direction` | direction of sorting | no |
| `filter_field` | filter by a given column | no |
| `filter_value` | value to filter by | no |


> **Note:** Default value for `sort_field` is `id`. All possible values are: 
> * `id`
> * `createdAt`
> * `updatedAt`

> **Note:** Default value for `direction` is `ASC`. All possible values are:
> * `ASC`
> * `DESC` 

> **Note:**  Possible values for `filter_field` are: 
> * `systemName`
> * `acknowledged`
> * `severity`

> **Note:**  If `filter_field` is set to `severity`, in addition to the possible values of `severity`, these values are possible for `filter_value`: 
> * `not_cleared` - Use to get all alarms that are not `cleared`

Example of valid invocation:
```json
GET /pde/alarm HTTP/1.1
Accept: application/json
```

Example of valid response:
```json
HTTP/1.1 200 OK
Content-Length: 324
Content-Type: application/json

{
	"count": 1,
	"data": [
		{
				"id": 1,
				"systemName": "Example producer with metadata",
				"acknowledged": false,
				"severity": "major",
				"description": "System not registered in Service Registry",
				"raisedAt": "2020-03-13T16:54:00.511Z",
				"modifiedAt": "2020-03-13T16:54:00.511Z"
			}
	]
}
```

### GET {baseURI}/alarm/{id}
 - __Interface:	GetPDAAlarm__
 - __Output: [PDEAlarm](#pdealarm)__
	 
Called to acquire the __[PDEAlarm](#pdealarm)__ specified by the `id` path parameter.

Example of valid invocation:
```json
GET /pde/alarm/1 HTTP/1.1
Accept: application/json
```

Example of valid response:
```json
HTTP/1.1 200 OK
Content-Length: 262
Content-Type: application/json

{
	"id": 1,
	"systemName": "Example producer with metadata",
	"acknowledged": false,
	"severity": "major",
	"description": "System not registered in Service Registry",
	"raisedAt": "2020-03-13T16:54:00.511Z",
	"modifiedAt": "2020-03-13T16:54:00.511Z"
}
```

### PATCH {baseURI}/alarm/{id}
 - __Interface:	UpdatePDAAlarm__
 - __Input: [PDEAlarmUpdate](#pdealarmupdate)__
 - __Output: [PDEAlarm](#pdealarm)__

Called to update the PDE Alarm specified by the `id` parameter with the information in the the request body.
The newly updated PDE Alarm is returned.

Example of valid invocation:
```json
GET /pde/alarm/1 HTTP/1.1
Accept: application/json
Content-Length: 29
Content-Type: application/json

{
	"acknowledged": true
}
```

Example of valid response:
```json
HTTP/1.1 200 OK
Content-Length: 309
Content-Type: application/json

{
	"id": 1,
	"systemName": "Example producer with metadata",
	"acknowledged": true,
	"severity": "major",
	"description": "System not registered in Service Registry",
	"raisedAt": "2020-03-13T16:54:00.511Z",
	"acknowledgedAt": "2020-03-13T17:32:00.531Z",
	"modifiedAt": "2020-03-13T17:32:00.531Z"
}
```


## Information model
Here, all data objects that can be part of PDA service calls are listed in alphabetic order.
As a complement to the explicitly defined types
in this section, there is also a list of implicit [primitive](#primitives) types.

### PDEAlarm
JSON object with the following fields:

| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `id` | Number | Id of the alarm | `true` | |
| `systemName` | String | Identity of the system | `true` | |
| `acknowledged` | Boolean | Has the alarm been acknowledged by an operator | `true` | |
| `severity` | String | One out of `indeterminate/critical/major/minor/warning/cleared` | `true` | |
| `description` | String | Description of the problem | `true` | |
| `raisedAt` | [DateTime](#alias-datetime--string) | When the alarm was first raised | `true` | |
| `updatedAt` | [DateTime](#alias-datetime--string) | When the alarm was last updated | `true` | |
| `clearedAt` | [DateTime](#alias-datetime--string) | When the alarm was cleared | `false` | |
| `acknowledgedAt` | [DateTime](#alias-datetime--string) | When the alarm was acknowledged | `false` | |


### PDEAlarmList
JSON object with the following fields:

| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `count` | Number | Number of records found | `true` | |
| `data` | Array\<[PDEAlarm](#pdealarm)> | Array of [PDE Alarms](#pdealarm) | `true` | |

### PDEAlarmUpdate
JSON object with the following fields. Currently only the following values can be updated. If a field is not present the current value will be used.

| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `acknowledged` | Boolean | Has the alarm been acknowledged by an operator |`false`||


### Primitives
As all messages are encoded using the [JSON] format,
the following primitive constructs, part of that standard, become available.
Note that the official standard is defined in terms of parsing rules, while this list only concerns
syntactic information. Furthermore, the Object and Array types are given optional generic type parameters,
which are used in this document to signify when pair values or elements are expected to conform to certain
types.

| JSON Type | Description |
| --------- | ----------- |
| Value | Any out of Object, Array, String, Number, Boolean or Null. |
| Object \<A> | An unordered collection of [String: Value] pairs, where each Value conforms to type A. |
| Array \<A> | An ordered collection of Value elements, where each element conforms to type A. |
| String | An arbitrary UTF-8 string. |
| Number | Any IEEE 754 binary64 floating point number, except for +Inf, -Inf and NaN. |
| Boolean | One out of `true` or `false`. |
| Null | Must be null. |

#### alias DateTime = String
Pinpoints a moment in time by providing a formatted string that conforms to the
[RFC 3339] specification, which could be regarded as a simplification of the ISO 8601
standard. Naively, the format could expressed as ”YYYY-MM-DDTHH:MM:SS.sssZ”,
where ”YYYY” denotes year (4 digits),
”MM” denotes month starting from 01,
”DD” denotes day starting from 01,
”HH” denotes hour in the 24-hour format (00-23),
”MM” denotes minute (00-59),
”SS” denotes second (00-59) and
”sss” denotes second fractions (000-999).
”T” is used as separator between the date and the time,
while ”Z” denotes the UTC time zone.
At least three fraction digits should be used, which gives millisecond precision. An example of a valid date/time string is ”2019-09-19T15:20:50.521Z”.
Other forms or variants, including the use of other time zones, is adviced against.

[HTTP]:https://doi.org/10.17487/RFC7230
[JSON]:https://doi.org/10.17487/RFC7159
[PDA]:plant-description-alarm-sd.md
[PDE]:plant-description-engine-sysd.md
[Plant Description Alarm]:plant-description-alarm-sd.md
[Plant Description Engine]:plant-description-engine-sysd.md
[RFC 3339]:https://doi.org/10.17487/RFC3339
[TLS]:https://doi.org/10.17487/RFC8446
