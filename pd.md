

 
# <a name="plantdescription">Plant description / Plant integrator</a>
 

 
## <a name="plantdescription_sdd">System Design Description Overview</a>

This supporting core system has the purpose of 



## <a name="plantdescription_usecases">Services and Use Cases</a>

Use case 1: 




## <a name="plantdescription_endpoints">Endpointssystems</a>


__PlantDescriptionEntry__  is the input


```json
{
	"plantDescription": "ArrowHead core",
	"systems": [
		{
			"systemName": "Service Registry",
			"ports": [
				{ "portName": "service_registry", "serviceDefinition": "Service Discovery"}	
			]
		},
		{
			"systemName": "Authorization",
			"ports": [
				{ "portName": "service_registry", "serviceDefinition": "Service Discovery", "conjugated": true },	
				{ "portName": "tokenGeneration", "serviceDefinition": "Token Generation"},
				{ "portName": "authorizationControl", "serviceDefinition": "Authorization Control"}	
			]
		},
		{
			"systemName": "Orchestraztion",
			"ports": [
				{ "portName": "service_registry", "serviceDefinition": "Service Discovery", "conjugated": true },	
				{ "portName": "tokenGeneration", "serviceDefinition": "Token Generation", "conjugated": true },
				{ "portName": "authorizationControl", "serviceDefinition": "Authorization Control", "conjugated": true },	
				{ "portName": "orchestrationService", "serviceDefinition": "OrchestrationService"},	
				{ "portName": "orchestrationStoreManagement", "serviceDefinition": "OrchestrationStoreManagement"},	
				{ "portName": "orchestrationPush", "serviceDefinition": "OrchestrationPush", "conjugated": true },
				{ "portName": "orchestrationCapabiliteis", "serviceDefinition": "OrchestrationCapabiliteis", "conjugated": true }	
			]
		},
		{
			"systemName": "Example producer with metadata",
			"metadata": {
				"additionalProp1": "string",
				"additionalProp2": "string",
				"additionalProp3": "string"
  			},
			"ports": [
				{ "portName": "service_registry", "serviceDefinition": "Service Discovery", "conjugated": true },	
				{ "portName": "serivePort", "serviceDefinition": "Provided Service"}	
			]
		}
		
	],
	"connections": [
		{ "consumer": { "systemName": "Authorization", "portName": "service_registry" },
		  "producer": { "systemName": "Service Registry", "portName": "service_registry" }},
		{ "consumer": { "systemName": "Orchestraztion", "portName": "service_registry" },
		  "producer": { "systemName": "Service Registry", "portName": "service_registry" }},
		{ "consumer": { "systemName": "Orchestraztion", "portName": "tokenGeneration" },
		  "producer": { "systemName": "Authorization", "portName": "tokenGeneration" }},
		{ "consumer": { "systemName": "Orchestraztion", "portName": "authorizationControl" },
		  "producer": { "systemName": "Authorization", "portName": "authorizationControl" }}

		{ "consumer": { "systemName": "Example producer with metadata", "portName": "service_registry" },
		  "producer": { "systemName": "Service Registry", "portName": "service_registry" }},
	]
```

### Plant description object

| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `plantDescription` | String | Identity of the plant description | true | | 
| `systems` | Array | List of System objects, see table below | true | |
| `connections` | Array | list of Connection objects between system ports, see table below | true | | 

### System object
| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `systemName` | String | Identity of the system | true | | 
| `metadata` | Object | Metadata - key-value pairs | false | |
| `ports` | Array | List of Port objects provided by the system, see table below | true | |

### Port object
| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `portName` | String | Identity of the port | true | |
| `serviceDefinition` | String | Service definition identity | true | |
| `conjugated` | Boolean | Is the port a consumer port | false | false |

### Connection object
| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `consumer` | Object | The consumer end SystemPort of the connection, see table below | true | | 
| `producer` | Object | The producer end SysetmPort of the connection, see table below | true | | 

### SystemPort object
| Field | Type | Description | Mandatory | Default value | 
| ----- | ---- | ----------- | --------- | ------------- |
| `systemName` | String | Identity of the system | true | | 
| `portName` | String | Identity of the port | true | |

