package eu.arrowhead.core.plantdescriptionengine;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.codec.json.JsonObject;

import static se.arkalix.dto.DtoCodec.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface Thing {
    JsonObject stuff();
}
