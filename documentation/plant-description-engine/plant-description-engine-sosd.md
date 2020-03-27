# Plant Description Engine - System of Systems Description (SosD)

## Abstract
This document describes how a system of systems the includes a Plant Description Engine (PDE) interacts with the PDE.

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
   
The four custom systems A-D also provides the [Monitorable] service. Furthermore, D also provides the [OrchestrationPush] service.


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
				{ "portName": "orchestrationPush", "serviceDefinition": "OrchestrationPush", "consumer": true },
				{ "portName": "orchestrationCapabiliteis", "serviceDefinition": "OrchestrationCapabiliteis", "consumer": true }	
			]
		},
		{
			"systemName": "Plant Description Engine",
			"ports": [
				{ "portName": "service_discovery", "serviceDefinition": "Service Discovery", "consumer": true },	
				{ "portName": "orchestrationService", "serviceDefinition": "OrchestrationService", "consumer": true },	
				{ "portName": "orchestrationStoreManagement", "serviceDefinition": "OrchestrationStoreManagement", "consumer": true },
				{ "portName": "orchestrationPush", "serviceDefinition": "OrchestrationPush"},	
				{ "portName": "orchestrationCapabilities", "serviceDefinition": "OrchestrationCapabilities"},	
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

		{ "consumer": { "systemName": "Orchestration", "portName": "orchestrationPush" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "orchestrationPush" }}
		{ "consumer": { "systemName": "Orchestration", "portName": "orchestrationCapabilities" },
		  "producer": { "systemName": "Plant Description Engine", "portName": "orchestrationCapabilities" }}
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

		{ "consumer": { "systemName": "Orchestration", "portName": "orchestrationPush" },
		  "producer": { "systemName": "D", "portName": "orchestrationPush" }},

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

The plant description shown above includes all links between systems in the plant. However, for some of those communication links the Orchestrator is not involved and therefore they are not required to be part of the plant description.

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

 

This supporting core system has the purpose of choreographing the consumers and producers in the plant (System of Systems / Local cloud).
An abstract view, on which systems the plant contains and how they are connected as consumers and producers, is used to populate the [Orchestrator] with store rules for each of the consumers. The abstract view does not contain any instance specific information, instead meta-data about each system is used to identify the service producers.

The plant description engine (PDE) can be configured, using the [Plant Description Management JSON] service, with several variants of the plant description of which at most one can be active.
The active plant description is used to populate the orchestrator and if no plant description is active the orchestrator does not contain any store rules populated by the PDE. This can be used to establish alternativ plants (plan A, plan B, etc).

The PDE gathers information about the presence of all specified systems in the active plant description. If a system is not present it raises an alarm. If it detects that an unknown system has registered a service in the service registry it also raises an alarm. For a consumer system to be monitored the system must produce the [Monitorable] service and hence also register in the service registry. The [Plant Description Monitor JSON] service can be used to inspect and manage any raised alarms.

Tentatively, in the future the PDE can gather system specific data from all systems in the plant that produces the [Monitorable] service. Furthermore, the PDE could collect information from an [Inventory]. Both of these additional data could then be returned by the [Plant Description Monitor JSON] service. 

## Services

The PDE produces three different services:
 + the [Monitorable JSON] service
 + the [Plant Description Management JSON] service
 + the [Plant Description Monitor JSON] service
 
The PDE consumes the following services:
 + the [Service Discovery] service produced by the [Service Registry] core system
 + the [Orchestration Store Management] service produced by the [Orchestrator] core system
 + the [Orchestration] service produced by the [Orchestrator] core system
 + the [AuthorizationControl] service produced by the [Authorization] core system
 + the [Inventory service] produced by an [Inventory] system (TBD)
 + the [Monitorable JSON] service produced by the systems in the plant (TBD)
    
  
[Authorization]:../../README.md#authorization
[AuthorizationControl]:../../README.md#authorization
[Inventory service]:TBD
[Inventory]:TBD
[Monitorable]:monitorable-sd.md
[Monitorable JSON]:monitorable-idd-http-json.md
[Orchestrator]:../../README.md#orchestrator
[Orchestration]:../../README.md#orchestrator
[Orchestration Store Management]:../../README.md#orchestrator
[OrchestrationPush]:../../README.md#orchestrator
[Plant Description Engine]:plant-description-sysd.md
[Plant Description Monitor]:plant-description-monitor-sd.md
[Plant Description Management]:plant-description-management-sd.md
[Service Discovery]:../../README.md#serviceregistry_usecases
[Service Registry]:../../README.md#serviceregistry
  