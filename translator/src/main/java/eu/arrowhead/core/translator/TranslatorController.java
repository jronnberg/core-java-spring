package eu.arrowhead.core.translator;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.core.translator.service.TranslatorService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.PathVariable;

@Api(tags = {CoreCommonConstants.SWAGGER_TAG_ALL})
@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS,
        allowedHeaders = {HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION}
)
@RestController
@RequestMapping(CommonConstants.TRANSLATOR_URI)
public class TranslatorController {

    //=================================================================================================
    // members
    private final Logger logger = LogManager.getLogger(TranslatorController.class);

    private static final String PATH_VARIABLE_ID = "id";
    private static final String PATH_ENTITY_ID = "entityId";
    private static final String PATH_ENTITY_TYPE = "entityType";
    private static final String PATH_ATTRIBUTE_NAME = "attrName";
    private static final String PATH_SERVICE_NAME = "serviceName";

    private static final String PATH_TRANSLATOR_ROOT = "/";
    private static final String PATH_TRANSLATOR_ALL = "/all";
    private static final String PATH_TRANSLATOR_BY_ID = "/{" + PATH_VARIABLE_ID + "}";

    private static final String PATH_TRANSLATOR_PLUGIN_ENTITY = "/plugin/service/{" + PATH_ENTITY_ID + "}";
    private static final String PATH_TRANSLATOR_PLUGIN_ENTITY_AND_SERVICE = "/plugin/service/{" + PATH_ENTITY_ID + "}/{" + PATH_SERVICE_NAME + "}";

    private static final String PATH_TRANSLATOR_FIWARE_ROOT = "/v2";
    private static final String PATH_TRANSLATOR_FIWARE_ENTITIES = PATH_TRANSLATOR_FIWARE_ROOT + "/entities";
    private static final String PATH_TRANSLATOR_FIWARE_ENTITIES_BY_ID = PATH_TRANSLATOR_FIWARE_ROOT + "/entities/{" + PATH_ENTITY_ID + "}";
    private static final String PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID = PATH_TRANSLATOR_FIWARE_ENTITIES_BY_ID + "/attrs";
    private static final String PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID_AND_ATTRIBUTE = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID + "/{"+ PATH_ATTRIBUTE_NAME + "}";
    private static final String PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTE_VALUE_BY_ID_AND_ATTRIBUTE = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID_AND_ATTRIBUTE + "/value";
    private static final String PATH_TRANSLATOR_FIWARE_TYPES = PATH_TRANSLATOR_FIWARE_ROOT + "/types";
    private static final String PATH_TRANSLATOR_FIWARE_TYPES_BY_TYPE = PATH_TRANSLATOR_FIWARE_TYPES + "/{"+PATH_ENTITY_TYPE+"}";
    
    
    @Autowired
    private TranslatorService translatorService;

    //=================================================================================================
    // methods
//-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Return an echo message with the purpose of testing the core service availability", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(CommonConstants.ECHO_URI)
    @ResponseBody
    public String echoService() {
        return "Got it!";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Simple test method to see if the http server where this resource is registered works or not", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_ROOT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getIt() {
        return "This is the Translator Arrowhead Core System";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "This method initiates the creation of a new translation hub, if none exists already, between two systems.", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(path = PATH_TRANSLATOR_ROOT, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String postTranslator(@RequestBody final String request) {
        return "POST: " + request;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to check all active hubs", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(PATH_TRANSLATOR_ALL)
    @ResponseBody
    public String getTranslatorList() {
        return "return all";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to check for a specific hub provided his translatorId", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(PATH_TRANSLATOR_BY_ID)
    @ResponseBody
    public String getTranslator(@PathVariable(value = PATH_VARIABLE_ID) final int translatorId) {
        return "return byt Id: " + translatorId;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to get Service from a System @ Translator-Plugin", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_PLUGIN_ENTITY_AND_SERVICE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String pluginGetEntityValue(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_SERVICE_NAME) final String serviceName) {
        return "GET: " + entityId + " - " + serviceName;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to create an entity Service from a System @ Translator-Plugin", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PutMapping(path = PATH_TRANSLATOR_PLUGIN_ENTITY_AND_SERVICE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String pluginCreateEntity(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_SERVICE_NAME) final String serviceName,
            @RequestBody final String request) {
        return "PUT: " + entityId + " - " + serviceName + "\nBody: " + request;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to update an entity Service from a System @ Translator-Plugin", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(path = PATH_TRANSLATOR_PLUGIN_ENTITY_AND_SERVICE, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String pluginUpdateEntity(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_SERVICE_NAME) final String serviceName,
            @RequestBody final String request) {
        return "POST: " + entityId + " - " + serviceName + "\nBody: " + request;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Public method to delete a Service from a System @ Translator-Plugin", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @DeleteMapping(path = PATH_TRANSLATOR_PLUGIN_ENTITY, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String pluginGetEntityValue(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId) {
        return "DELETE: " + entityId;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE endpoints", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ROOT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareGetIt() {
        return "Return entities: ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE list Entities", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareListEntities(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String idPattern,
            @RequestParam(required = false) String typePattern,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mq,
            @RequestParam(required = false) String georel,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String coords,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String options
    ) {
        return "GET: List Entities ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE create Entity", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareCreateEntity(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String idPattern,
            @RequestParam(required = false) String typePattern,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mq,
            @RequestParam(required = false) String georel,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String coords,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String options,
            @RequestBody final String request
    ) {
        return "POST: List Entities ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE retrieve Entity", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareRetrieveEntity(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options
    ) {
        return "GET: fiwareRetrieveEntity ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE retrieve Entity Attributes", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareRetrieveEntityAttributes(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options
    ) {
        return "GET: fiwareRetrieveEntityAttributes ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE update/append Entity Attributes", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareupdateAppendEntityAttributes(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options,
            @RequestBody final String request
    ) {
        return "POST: fiwareupdateAppendEntityAttributes ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE update existing Entity Attributes", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PatchMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareUpdateExistingEntityAttributes(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options,
            @RequestBody final String request
    ) {
        return "PATCH: fiwareUpdateExistingEntityAttributes ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE replace all existing Entity Attributes", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PutMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareReplaceAllEntityAttributes(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options,
            @RequestBody final String request
    ) {
        return "PUT: fiwareReplaceAllEntityAttributes ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE remove Entity", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @DeleteMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_BY_ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareRemoveEntity(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attrs,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) String options
    ) {
        return "DELETE: fiwareRemoveEntity ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE get Entity Attribute data", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID_AND_ATTRIBUTE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareGetAttributeData(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_ATTRIBUTE_NAME) final String attrName,
            @RequestParam(required = false) String type
    ) {
        return "GET: fiwareGetAttributeData ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE update Entity Attribute data", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PutMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID_AND_ATTRIBUTE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareUpdateAttributeData(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_ATTRIBUTE_NAME) final String attrName,
            @RequestParam(required = false) String type,
            @RequestBody final String request
    ) {
        return "PUT: fiwareUpdateAttributeData ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE remove a single Entity Attribute data", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @DeleteMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTES_BY_ID_AND_ATTRIBUTE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareRemoveASingleAttribute(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_ATTRIBUTE_NAME) final String attrName,
            @RequestParam(required = false) String type
    ) {
        return "DELETE: fiwareRemoveASingleAttribute ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE get Entity Attribute value", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTE_VALUE_BY_ID_AND_ATTRIBUTE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareGetAttributeValue(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_ATTRIBUTE_NAME) final String attrName,
            @RequestParam(required = false) String type
    ) {
        return "GET: fiwareGetAttributeValue ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE update Entity Attribute value", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PutMapping(path = PATH_TRANSLATOR_FIWARE_ENTITIES_ATTRIBUTE_VALUE_BY_ID_AND_ATTRIBUTE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareUpdateAttributeValue(
            @PathVariable(value = PATH_ENTITY_ID) final String entityId,
            @PathVariable(value = PATH_ATTRIBUTE_NAME) final String attrName,
            @RequestParam(required = false) String type,
            @RequestBody final String request
    ) {
        return "PUT: fiwareUpdateAttributeValue ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE list Entity Types", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_TYPES, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareListEntityTypes(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String options
    ) {
        return "GET: fiwareListEntityTypes ";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "FIWARE Retrieve Entity Type", response = String.class, tags = {CoreCommonConstants.SWAGGER_TAG_CLIENT})
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = PATH_TRANSLATOR_FIWARE_TYPES_BY_TYPE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fiwareRetrieveEntityType(
            @PathVariable(value = PATH_ENTITY_TYPE) final String entityType
    ) {
        return "GET: fiwareRetrieveEntityType ";
    }
    

    //=================================================================================================
    // assistant methods
    //-------------------------------------------------------------------------------------------------
    /*private String testMethod() {
        logger.info("testMethod started...");

        return "testMethod";
    }*/
}
