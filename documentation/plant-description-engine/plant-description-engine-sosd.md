# Plant Description Engine - System of Systems Description (SosD)

## Abstract
This document describes how a system of systems that includes a [Plant Description Engine] (PDE) interacts with the PDE.

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

The plant description shown above includes all links between systems in the plant. However, for some of those communication links the Orchestrator is not called and therefore they are not strictly required in the plant description.

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


## Interactions between systems

To setup a fully functional system of systems that includes a PDE there are a number of message interaction between the systems necessary.

### Service registration for systems that produce a service

Whenever a systems that provides a service, starts up if must register its services in the [Service Registry]. To do this is locates the Service Registry either by configuration or by using the [DNS]. The system connects to the [Service Discovery] service and uses the [Register](../../README.md#serviceregistry_endpoints_post_register) end point to register its provided services. The system also uses the [Query](../../README.md#serviceregistry_endpoints_post_query) end point to retrieve information about the [Authorization] system. Especially the public key in the `authenticationInfo` is needed to verify authorization tokens when other systems connect to its service.

### Service lookup for systems that consumes a service

Whenever a systems that should consume a service, starts up if must locate the [Orchestration] end point to query the [Orchestrator] about which systems to connect to. This is done by locating the Service Registry either by configuration or by using the [DNS]. The system then connects to the [Service Discovery] service and uses the [Query](../../README.md#serviceregistry_endpoints_post_query) to retrieve information about the [Orchestration] service.

It then connects to the Orchestrator and uses the [Orchestration] end point to query about each specific service it needs. The Orchestrator in turn queries the Service Discovery service for providers of the requested service. Using its store rules the Orchestrator selects the systems that should be used to provide the service. It then uses the [Authorization] system's [Check an Intracloud rule](../../README.md#authorization_endpoints_post_intracloud_check) end point to authorize the consuming system and the [Genrate Token](../../README.md#authoritation_endpoints_post_token) end point to generate the needed token for each of the providing systems.

When the consuming system receives an Orchestration Response from the Orchestrator it decodes the authorization tokens received and connects to the providing system sending along the decrypted authorization token which is checked by the provider using the Authorization systems public key. If that matches the connection between consumer and provider is established.


### Updating the Plant description

The PDE should contain at least one Plant Description (PD) of the plant. The operator (operation center system) uses the Orchestrator to lookup the system (PDE) providing the [Plant Description Management] service. Then connects to the PDE and uses the [AddPlantDescription](plant-description-management-sd.md#interface-addplantdescriptionplantdescription-plantdescriptionentrylist) end point to add a PD.
When the operator has activated a PD using {[Add](plant-description-management-sd.md#interface-addplantdescriptionplantdescription-plantdescriptionentrylist)/[Replace](plant-description-management-sd.md#interface-replaceplantdescriptionid-plantdescription-plantdescriptionentry)/[Update](plant-description-management-sd.md#interface-updateplantdescriptionid-plantdescriptionupdate-plantdescriptionentry)}PlantDescription end point, the PDE connects to the [Orchestration Store Management] service.

The PDE then start with removing any store rules, using the [Delete Store Entries by ID](../../README.md#orchestrator_endpoints_delete_store_id) end point, that it has previously stored which should no longer be present. These removed store rules was added for another active PD. It then adds new store rules, using the  [Add Store Entries](../../README.md#orchestrator_endpoints_post_store) end point, for all the connections present in the newly activated PD.
 
Whenever the Orchestrator is updated it uses the [Orchestration Push] service, of all the systems that has registered as a producer of that service, to inform them about any updates that concerns them. If a consumer system has not registered the [Orchestration Push] service it must poll the Orchestrator regurlarly to keep updated.
 
### Monitoring the plant
 
The PDE queries the Orchestrator about any systems that it should monitor using the [Monitorable] service. The PDE regularly [Ping](monitorable-sd.md#interface-ping-ok) the monitored systems and raises an alarm if the system does not respond. 

If the monitored system provides any [SystemData](monitorable-sd.md#interface-getsystemdata-systemdata) this data is stored by the PDE and returned as part of the Plant Description Entries provided by the [Plant Description Monitor] service.

If there is an Inventory system present in the plant that produces the [Inventory] service and the PDE is connected to it in the active PD, the PDE connects to the [Inventory] service. The PDE queries it for [InventoryData] for the systems that the PDE monitors, according to the active PD. If the system has provided an [Inventory ID](monitorable-sd.md#interface-getinventoryid-inventoryid) this is used in the Inventory query otherwise only MetaData about the system is used. Any found [InventoryData] is stored by the PDE and returned as part of the Plant Description Entries provided by the [Plant Description Monitor] service.



   
  
[Authorization]:../../README.md#authorization
[AuthorizationControl]:../../README.md#authorization
[DNS]:https://en.wikipedia.org/wiki/Domain_Name_System
[Inventory]:inventory-sd.md
[InventoryData]:inventory-sd.md#struct-inventorydata
[Monitorable]:monitorable-sd.md
[Orchestrator]:../../README.md#orchestrator
[Orchestration]:../../README.md#orchestrator_endpoints_post_orchestration
[Orchestration Store Management]:../../README.md#orchestrator
[Orchestration Push]:../../README.md#orchestrator_usecases
[Plant Description Engine]:plant-description-engine-sysd.md
[Plant Description Monitor]:plant-description-monitor-sd.md
[Plant Description Management]:plant-description-management-sd.md
[Service Discovery]:../../README.md#serviceregistry_usecases
[Service Registry]:../../README.md#serviceregistry
  