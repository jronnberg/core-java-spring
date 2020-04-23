package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * A so called Data Transfer Object (DTO) interface. Such an interface may
 * only contain getters (methods that return something other than "void"
 * and accept no parameters) and is used to automatically generate classes
 * for instantiating, encoding and decoding Java objects that satisfies the
 * interface.
 * <p>
 * This particular DTO interface causes the "PingData" and "PingBuilder"
 * classes to be generated with support for reading and writing "PingData"
 * objects from/to JSON.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
interface Ping {
    /**
     * Any string.
     */
    String ping();

    /**
     * An optional identifier.
     */
    Optional<String> id();

    /**
     * Optional fields that carry anything else than arrays, lists or maps
     * must use the {@link Optional} type to indicate that they are indeed
     * optional.
     * <p>
     * DTO interfaces may include other DTO interfaces, as long as also
     * they have the same @Readable and @Writable annotations with the same
     * specified encodings.
     * <p>
     * The complete list of built-in types that are supported in DTO
     * interfaces can be read in the
     * {@code se.arkalix.dto.DtoPropertyFactory} class in the
     * "kalix-processors" module.
     */
    Optional<Instant> timestamp();
    List<Integer> stuff();

    /**
     * Any number of default methods may be specified without having any impact
     * on the concrete fields that will be part of the generated DTO classes.
     */
    default String asString() {
        String result =  "Ping[ping=" + ping() + ",id=" + id().orElse("null") +
            ",timestamp=" + timestamp().map(Instant::toString).orElse("null") +
            ", stuff: ";
            for (var x : stuff())
                result += "," + x;
            result += "]";
        return result;
    }
}
