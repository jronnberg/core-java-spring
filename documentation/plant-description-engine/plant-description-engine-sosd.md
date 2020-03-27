# Plant Description Engine - System of Systems Description (SosD)

## Abstract
This document describes how a system of systems that includes a Plant Description Engine (PDE) interacts with the PDE.

## Overview

This document describes how a system of systems that includes the Plant Description Engine (PDE) Arrowhead Framework system interacts.

A plant (System of Systems / Local cloud) is assumed to include the following systems:
 - [Service Registry]
 - [Authorization]
 - [Orchestrator]
 - [Plant Description Engine]
 - Some Producer systems
 - Some Consumer systems

We will as an example consider a plant that has the four core systems and 5 custom systems:
 - Operations Center that manages and monitors the Plant Description Engine
 - A that produces service X
 - B that produces service Y
 - C that produces service Z
 - D that consumes services X, Y and Z.
   This system should always be connected to a system producing X. 
   It should also be connected to either a system that provides Y or a system the provides Z but not both. 
   
The four custom systems A-D also provides the [Monitorable] service.


## The plant description(s) describing the example system

The basic Arrow Head core systems are connected to each other

IMAGE

This corresponds to a Plant Description with the core systems:

```json
{
	"id": 1,
	"plantDescription": "ArrowHead core",
	"systems": [
		{
			"systemName": "Service Registry",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery"}	
			]
		},
		{
			"systemName": "Authorization",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "tokenGeneration", "serviceDefinition": "Token Generation"},
				{ "portName": "authorizationControl", "serviceDefinition": "Authorization Control"}	
			]
		},
		{
			"systemName": "Orchestration",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "tokenGeneration", "serviceDefinition": "Token Generation", "consumer": true },
				{ "portName": "authorizationControl", "serviceDefinition": "Authorization Control", "consumer": true },	
				{ "portName": "orchestrationService", "serviceDefinition": "OrchestrationService"},	
				{ "portName": "orchestrationStoreManagement", "serviceDefinition": "OrchestrationStoreManagement"},	
true }	
			]
		},
		{
			"systemName": "Plant Description Engine",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "orchestrationService", "serviceDefinition": "OrchestrationService", "consumer": true },	
				{ "portName": "orchestrationStoreManagement", "serviceDefinition": "OrchestrationStoreManagement", "consumer": true },
				{ "portName": "monitor", "serviceDefinition": "Plant Description Monitor"},
				{ "portName": "management", "serviceDefinition": "Plant Description Management"},
			]
		},
		
	],
	"connections": [
		{ "consumer": { "systemName": "Authorization", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},

		{ "consumer": { "systemName": "Orchestration", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},
		{ "consumer": { "systemName": "Orchestration", "portName": "tokenGeneration" },
		  "producer": { "systemName": "Authorization", "portName": "tokenGeneration" }},
		{ "consumer": { "systemName": "Orchestration", "portName": "authorizationControl" },
		  "producer": { "systemName": "Authorization", "portName": "authorizationControl" }},

		{ "consumer": { "systemName": "Plant Description Engine", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "orchestrationService" },
		  "producer": { "systemName": "Orchestration", "portName": "orchestrationService" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "orchestrationStoreManagement" },
		  "producer": { "systemName": "Orchestration", "portName": "orchestrationStoreManagement" }}
	]
}
```

All custom systems are assumed to be connected to the core systems and register their provides services in the [Service Registry].

IMAGE

This corresponds to a bare plant description that include the core and contains the custom systems and how they are connected to the core systems: 

```json
{
	"id": 2,
	"plantDescription": "Example plant - bare",
	"include": [ 1 ],
	"systems": [
		{
			"systemName": "Operations Center",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "orchestrationService", "serviceDefinition": "OrchestrationService", "consumer": true },	
				{ "portName": "monitor", "serviceDefinition": "Plant Description Monitor", "consumer": true },
				{ "portName": "management", "serviceDefinition": "Plant Description Management", "consumer": true }
			]
		},
		{
			"systemName": "A",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "x", "serviceDefinition": "X"},	
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "B",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "y", "serviceDefinition": "Y"},	
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "C",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "z", "serviceDefinition": "Z"},
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "D",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
				{ "portName": "x", "serviceDefinition": "X", "consumer": true }	
				{ "portName": "y", "serviceDefinition": "Y", "consumer": true }	
				{ "portName": "z", "serviceDefinition": "Z", "consumer": true }	
			]
		}
		
	],
	"connections": [
		{ "consumer": { "systemName": "A", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},

		{ "consumer": { "systemName": "B", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},

		{ "consumer": { "systemName": "C", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},


		{ "consumer": { "systemName": "D", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},
		{ "consumer": { "systemName": "D", "portName": "orchestrationService" },
		  "producer": { "systemName": "Orchestration", "portName": "orchestrationService" }},

		{ "consumer": { "systemName": "Operations Center", "portName": "service_discovery" },
		  "producer": { "systemName": "Service Registry", "portName": "service_discovery" }},
		{ "consumer": { "systemName": "Operations Center", "portName": "orchestrationService" },
		  "producer": { "systemName": "Orchestration", "portName": "orchestrationService" }},
	]
}
```

The bare plant is extended with the connections that should always be there

```json
{
	"id": 3,
	"plantDescription": "Example plant - base",
	"include": [ 2 ],
	"systems": [],
	"connections": [
		{ "consumer": { "systemName": "D", "portName": "x" },
		  "producer": { "systemName": "A", "portName": "x" }},

		{ "consumer": { "systemName": "Operations Center", "portName": "management" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "management" }},
		{ "consumer": { "systemName": "Operations Center", "portName": "monitor" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "monitor" }},

		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "A", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "B", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "C", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "D", "portName": "monitorable" }},

	]
}
```


We now end up with two different variants that include the base variant, one that connects D to service Y in B 

```json
{
	"id": 4,
	"plantDescription": "Example plant variant 1",
	"active": true,
	"include": [ 3 ],
	"systems": [],
	"connections": [
		{ "consumer": { "systemName": "D", "portName": "y" },
		  "producer": { "systemName": "B", "portName": "y" }}
	]
}
```

and one that connects D to service Z in C

```json
{
	"id": 5,
	"plantDescription": "Example plant variant 2",
	"include": [ 3 ],
	"systems": [],
	"connections": [
		{ "consumer": { "systemName": "D", "portName": "z" },
		  "producer": { "systemName": "C", "portName": "z" }}
	]
}
```


### Bootstrapping problem
In the plant description above we have two bootstrapping problems. First in order for the Operations Center to be allowed to use the Plant Description Management service from the PDE a rule allowing it to do so must be added to the Orchestrator. Otherwise it will not be able to add the first plant description allowing it to use the service.   

Secondly, in order for the PDE to be allowed to use the OrchestrationStoreManagement service from the Orchestrator a rule allowing it to do so should be added to the Orchestrator. (This might not be needed if all core systems are allowed to use any service from other core systems)


### Simplified plant description

The plant description shown above includes all links between systems in the plant. However, for some of those communication links the Orchestrator is not involved and therefore they are not strictly required to be part of the plant description.

A merged and cleaned plant description would look like this

```json
{
	"id": 3,
	"plantDescription": "Example plant - base",
	"systems": [
		{
			"systemName": "Plant Description Engine",
			"ports": [
				{ "portName": "monitor", "serviceDefinition": "Plant Description Monitor"},
				{ "portName": "management", "serviceDefinition": "Plant Description Management"},
				{ "portName": "monitorable", "serviceDefinition": "Monitorable", "consumer": true }
			]
		},
		{
			"systemName": "Operations Center",
			"ports": [
				{ "portName": "monitor", "serviceDefinition": "Plant Description Monitor", "consumer": true },
				{ "portName": "management", "serviceDefinition": "Plant Description Management", "consumer": true }
			]
		},
		{
			"systemName": "A",
			"ports": [
				{ "portName": "x", "serviceDefinition": "X"},	
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "B",
			"ports": [
				{ "portName": "y", "serviceDefinition": "Y"},	
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "C",
			"ports": [
				{ "portName": "z", "serviceDefinition": "Z"},
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
			]
		},
		{
			"systemName": "D",
			"ports": [
				{ "portName": "monitorable", "serviceDefinition": "Monitorable"}	
				{ "portName": "x", "serviceDefinition": "X", "consumer": true }	
				{ "portName": "y", "serviceDefinition": "Y", "consumer": true }	
				{ "portName": "z", "serviceDefinition": "Z", "consumer": true }	
			]
		}
		
	],
	"connections": [
		{ "consumer": { "systemName": "D", "portName": "x" },
		  "producer": { "systemName": "A", "portName": "x" }},

		{ "consumer": { "systemName": "Operations Center", "portName": "management" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "management" }},
		{ "consumer": { "systemName": "Operations Center", "portName": "monitor" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "monitor" }},

		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "A", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "B", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "C", "portName": "monitorable" }},
		{ "consumer": { "systemName": "Plant Description Engine", "portName": "monitorable" },
		  "producer": { "systemName": "D", "portName": "monitorable" }}
	]
}
```

 
   
  
[Authorization]:../../README.md#authorization
[AuthorizationControl]:../../README.md#authorization
[Inventory service]:TBD
[Inventory]:TBD
[Monitorable]:monitorable-sd.md
[Monitorable JSON]:monitorable-idd-http-json.md
[Orchestrator]:../../README.md#orchestrator
[Orchestration]:../../README.md#orchestrator
[Orchestration Store Management]:../../README.md#orchestrator
[Plant Description Engine]:plant-description-sysd.md
[Plant Description Monitor]:plant-description-monitor-sd.md
[Plant Description Management]:plant-description-management-sd.md
[Service Discovery]:../../README.md#serviceregistry_usecases
[Service Registry]:../../README.md#serviceregistry
  